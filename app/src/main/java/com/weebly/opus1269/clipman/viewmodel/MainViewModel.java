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
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.repos.MainRepo;

import org.threeten.bp.Instant;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** ViewModel for MainActvity */
public class MainViewModel extends BaseRepoViewModel<MainRepo> {
  /** Clips list */
  @NonNull
  private final MediatorLiveData<List<Clip>> clips;

  /** Labels list */
  @NonNull
  private final MediatorLiveData<List<Label>> labels;

  /** Clips that were deleted */
  @NonNull
  private final MutableLiveData<List<Clip>> undoClips;

  /** Last selected Clip */
  @Nullable
  private Clip lastSelClip;

  /** Map Label name to Label */
  @NonNull
  private final Map<String, Label> labelsMap;

  public MainViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    lastSelClip = null;

    undoClips = new MutableLiveData<>();
    undoClips.setValue(null);

    clips = new MediatorLiveData<>();
    clips.setValue(null);
    clips.addSource(mRepo.getClips(), clips::setValue);

    labels = new MediatorLiveData<>();
    labels.setValue(null);
    labels.addSource(mRepo.getLabels(), labels::setValue);

    labelsMap = new HashMap<>(0);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    setClipTextFilter("");
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setInfoMessage(null);
    mRepo.setErrorMsg(null);
  }

  @NonNull
  public LiveData<List<Clip>> getClips() {
    return clips;
  }

  @NonNull
  public LiveData<List<Label>> getLabels() {
    return labels;
  }

  @NonNull
  public LiveData<Clip> getSelClip() {
    return mRepo.getSelClip();
  }

  public void setSelClip(@Nullable Clip clip) {
    mRepo.setSelClip(clip);
  }

  @Nullable
  public Clip getSelClipSync() {
    return mRepo.getSelClip().getValue();
  }

  @NonNull
  public LiveData<List<Label>> getSelLabels() {
    return mRepo.getSelLabels();
  }

  @NonNull
  public LiveData<List<Clip>> getUndoClips() {
    return undoClips;
  }

  public void setUndoClips(@Nullable List<Clip> clips) {
    undoClips.setValue(clips);
  }

  @Nullable
  public Clip getLastSelClip() {
    return lastSelClip;
  }

  public void setLastSelClip(@Nullable Clip lastSelClip) {
    this.lastSelClip = lastSelClip;
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

  public LiveData<Label> getFilterLabel() {
    return mRepo.getFilterLabel();
  }

  @NonNull
  public String getFilterLabelNameSync() {
    final Label label = mRepo.getFilterLabel().getValue();
    return (label == null) ? "" : label.getName();
  }

  public void setFilterLabelName(@NonNull String filterLabelName) {
    final Label label = labelsMap.get(filterLabelName);
    mRepo.setFilterLabel(label);
  }

  public boolean isVisible(@NonNull Clip clip) {
    boolean ret = false;
    final List<Clip> clips = this.clips.getValue();
    if (!AppUtils.isEmpty(clips)) {
      if (clips.contains(clip)) {
        ret = true;
      }
    }
    return ret;
  }

  /**
   * Add a clip
   * @param clip Clip
   */
  public void addClip(@Nullable Clip clip) {
    if (clip != null) {
      mRepo.addClip(clip);
    }
  }

  /**
   * Remove a clip, save for undo
   * @param clip Clip
   */
  public void removeClip(@Nullable Clip clip) {
    if (clip != null) {
      mRepo.removeClip(clip);
      undoClips.postValue(Collections.singletonList(clip));
    }
  }

  /** Delete selected clip */
  public void removeSelClip() {
    final Clip clip = getSelClipSync();
    if (clip != null) {
      removeClip(clip);
      setSelClip(null);
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
    final List<Clip> clips = undoClips.getValue();
    if (clips != null) {
      mRepo.addClips(clips);
    }
  }

  /** Undo the last delete and select it */
  public void undoDeleteAndSelect() {
    App.getExecutors().diskIO().execute(() -> {
      final List<Clip> clips = undoClips.getValue();
      if (!AppUtils.isEmpty(clips)) {
        mRepo.addClipSync(clips.get(0));
        setSelClip(clips.get(0));
      }
    });
  }

  /** Copy selected clip to the Clipboard */
  public void copySelClip() {
    final Clip clip = getSelClipSync();
    if (!Clip.isWhitespace(clip)) {
      clip.setRemote(false);
      clip.setDate(Instant.now().toEpochMilli());
      clip.copyToClipboard(getApplication());
      setInfoMessage(getApplication().getString(R.string.clipboard_copy));
    }
  }

  /** Toggle the favorite state of selected clip */
  public void toggleSelFavorite() {
    final Clip clip = getSelClipSync();
    if (clip != null) {
      clip.setFav(!clip.getFav());
      mRepo.updateClipFav(clip);
    }
  }

  public void setLabelsMap(@Nullable List<Label> labels) {
    labelsMap.clear();
    if (!AppUtils.isEmpty(labels)) {
      for (final Label label : labels) {
        labelsMap.put(label.getName(), label);
      }
    }
  }
}
