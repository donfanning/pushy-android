/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.util.Collections;
import java.util.List;

/** ViewModel for MainActvity */
public class MainViewModel extends BaseRepoViewModel<MainRepo> implements
  SharedPreferences.OnSharedPreferenceChangeListener {
  /** Clips that were deleted */
  @NonNull
  public final MutableLiveData<List<ClipEntity>> undoClips;

  /** Clips list */
  @NonNull
  private final MediatorLiveData<List<ClipEntity>> clips;

  /** Selected Clip */
  @NonNull
  private final MediatorLiveData<ClipEntity> selectedClip;

  /** Last selected Clip */
  public ClipEntity lastSelectedClip;

  /** Sort with favorites first if true */
  public boolean pinFavs;

  /** Show only favorites if true */
  public boolean filterByFavs;

  /** Show only Clips with the given label if non-whitespace */
  @NonNull
  public String labelFilter;

  /** Sort by date or text */
  private int sortType;

  /** Clip Source */
  @Nullable
  private LiveData<ClipEntity> selectedClipSource;

  /** Clips Source */
  @NonNull
  private LiveData<List<ClipEntity>> clipsSource;

  public MainViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    lastSelectedClip = null;

    selectedClipSource = null;

    selectedClip = new MediatorLiveData<>();
    selectedClip.setValue(null);

    pinFavs = Prefs.INST(app).isPinFav();

    filterByFavs = Prefs.INST(app).isFavFilter();

    labelFilter = Prefs.INST(app).getLabelFilter();

    sortType = Prefs.INST(app).getSortType();

    undoClips = new MutableLiveData<>();
    undoClips.setValue(null);

    clips = new MediatorLiveData<>();
    clips.setValue(null);
    clipsSource = mRepo.getClips(filterByFavs, pinFavs, sortType);
    clips.addSource(clipsSource, clips::setValue);

    // listen for preference changes
    PreferenceManager
      .getDefaultSharedPreferences(app)
      .registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    // stop listening for preference changes
    PreferenceManager
      .getDefaultSharedPreferences(getApplication())
      .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setErrorMsg(null);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                        String key) {
    final Context context = getApplication();
    // TODO do something with labelfilter

    //noinspection IfCanBeSwitch
    if (Prefs.INST(context).PREF_FAV_FILTER.equals(key)) {
      filterByFavs = Prefs.INST(context).isFavFilter();
      final ClipEntity clip = getSelectedClipSync();
      if (filterByFavs && (clip != null) && !clip.getFav()) {
        // unselect if we will be filtered out
        setSelectedClip(null);
      }
    } else if (Prefs.INST(context).PREF_PIN_FAV.equals(key)) {
      pinFavs = Prefs.INST(context).isPinFav();
    } else if (Prefs.INST(context).PREF_SORT_TYPE.equals(key)) {
      sortType = Prefs.INST(context).getSortType();
    } else if (Prefs.INST(context).PREF_LABEL_FILTER.equals(key)) {
      labelFilter = Prefs.INST(context).getLabelFilter();
    } else {
      // not ours to handle
      return;
    }

    Log.logD(TAG, "source changed");
    clips.removeSource(clipsSource);
    clipsSource = mRepo.getClips(filterByFavs, pinFavs, sortType);
    clips.addSource(clipsSource, clips::setValue);
  }

  @NonNull
  public LiveData<List<ClipEntity>> getClips() {
    return clips;
  }

  @NonNull
  public LiveData<ClipEntity> getSelectedClip() {
    return selectedClip;
  }

  public void setSelectedClip(@Nullable ClipEntity clip) {
    if (selectedClipSource != null) {
      selectedClip.removeSource(selectedClipSource);
    }
    lastSelectedClip = selectedClip.getValue();
    if (ClipEntity.isWhitespace(clip)) {
      // no clip
      selectedClipSource = null;
      selectedClip.setValue(null);
    } else {
      selectedClipSource = mRepo.getClip(clip.getId());
      selectedClip.addSource(selectedClipSource, this.selectedClip::setValue);
    }
  }

  @Nullable
  public ClipEntity getSelectedClipSync() {
    return selectedClip.getValue();
  }

  /**
   * Add a clip
   * @param clip Clip
   * @return true if added
   */
  public boolean addClipSync(@Nullable ClipEntity clip) {
    boolean ret = false;
    if (clip != null) {
      final long id = mRepo.addClipSync(clip);
      if (id != -1L) {
        clip.setId(id);
        ret = true;
      }
    }
    return ret;
  }

  /**
   * Remove a clip, save for undo
   * @param clip Clip
   */
  public void removeClip(@Nullable ClipEntity clip) {
    if (clip != null) {
      mRepo.removeClip(clip);
      undoClips.postValue(Collections.singletonList(clip));
    }
  }

  /**
   * Remove all clips, save for undo
   * @param includeFavs if true, remove favorites too
   */
  public void removeAll(boolean includeFavs) {
    setIsWorking(true);
    App.getExecutors().diskIO().execute(() -> {
      undoClips.postValue(mRepo.removeAllClipsSync(includeFavs));
      postIsWorking(false);
    });
  }

  /** Undo the last delete */
  public void undoDelete() {
    mRepo.addClips(undoClips.getValue());
  }
}
