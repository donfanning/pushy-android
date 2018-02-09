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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

import org.threeten.bp.Instant;

import java.util.Collections;
import java.util.List;

/** ViewModel for MainActvity */
public class MainViewModel extends BaseRepoViewModel<MainRepo> {
  /** Clips list */
  @NonNull
  private final MediatorLiveData<List<ClipEntity>> clips;

  /** Clips that were deleted */
  @NonNull
  private final MutableLiveData<List<ClipEntity>> undoClips;

  /** Last selected Clip */
  @Nullable
  private ClipEntity lastSelectedClip;

  public MainViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    lastSelectedClip = null;

    undoClips = new MutableLiveData<>();
    undoClips.setValue(null);

    clips = new MediatorLiveData<>();
    clips.setValue(null);
    clips.addSource(mRepo.getClips(), clips::setValue);
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setInfoMessage(null);
    mRepo.setErrorMsg(null);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    setClipTextFilter("");
  }

  @NonNull
  public LiveData<List<ClipEntity>> getClips() {
    return clips;
  }

  @NonNull
  public LiveData<ClipEntity> getSelectedClip() {
    return mRepo.getSelectedClip();
  }

  public void setSelectedClip(@Nullable ClipEntity clip) {
    setLastSelectedClip(getSelectedClipSync());
    mRepo.setSelectedClip(clip);
  }

  @Nullable
  public ClipEntity getSelectedClipSync() {
    return mRepo.getSelectedClipSync();
  }

  @NonNull
  public LiveData<List<ClipEntity>> getUndoClips() {
    return undoClips;
  }

  public void setUndoClips(@Nullable List<ClipEntity> clips) {
    undoClips.setValue(clips);
  }

  @Nullable
  public ClipEntity getLastSelectedClip() {
    return lastSelectedClip;
  }

  private void setLastSelectedClip(@Nullable ClipEntity lastSelectedClip) {
    this.lastSelectedClip = lastSelectedClip;
  }

  @NonNull
  public LiveData<String> getClipTextFilter() {
    return mRepo.getClipTextFilter();
  }

  @NonNull
  public String getClipTextFilterSync() {
    return mRepo.getClipTextFilterSync();
  }

  public void setClipTextFilter(@NonNull String query) {
    mRepo.setClipTextFilter(query);
  }

  /**
   * Add a clip
   * @param clip Clip
   */
  public void addClip(@Nullable ClipEntity clip) {
    if (clip != null) {
      mRepo.addClip(clip);
    }
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

  /** Delete selected clip */
  public void removeSelectedClip() {
    final ClipEntity clip = getSelectedClipSync();
    if (clip != null) {
      removeClip(clip);
      setSelectedClip(null);
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
    final List<ClipEntity> clips = undoClips.getValue();
    if (clips != null) {
      mRepo.addClips(clips);
    }
  }

  /** Undo the last delete and select it */
  public void undoDeleteAndSelect() {
    App.getExecutors().diskIO().execute(() -> {
      final List<ClipEntity> clips = undoClips.getValue();
      if (!AppUtils.isEmpty(clips)) {
        mRepo.addClipSync(clips.get(0));
        setSelectedClip(clips.get(0));
      }
    });
  }

  /** Copy selected clip to the Clipboard */
  public void copySelectedClip() {
    final ClipEntity clip = getSelectedClipSync();
    if (!ClipEntity.isWhitespace(clip)) {
      clip.setRemote(false);
      clip.setDate(Instant.now().toEpochMilli());
      clip.copyToClipboard(getApplication());
      setInfoMessage(getApplication().getString(R.string.clipboard_copy));
    }
  }

  /** Toggle the favorite state of selected clip */
  public void toggleSelectedFavorite() {
    final ClipEntity clip = getSelectedClipSync();
    if (clip != null) {
      clip.setFav(!clip.getFav());
      mRepo.updateClipFav(clip);
    }
  }
}
