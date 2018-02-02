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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** ViewModel for a {@link ClipEntity} */
public class ClipViewerViewModel extends BaseRepoViewModel<MainRepo> {
  /** Our Clip */
  @NonNull
  private final MediatorLiveData<ClipEntity> clip;

  /** Our highlighted text */
  @NonNull
  private String highlight;

  /** Clip Source */
  private LiveData<ClipEntity> clipSource;

  public ClipViewerViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    highlight = "";

    this.clipSource = null;

    this.clip = new MediatorLiveData<>();
    this.clip.setValue(null);
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setErrorMsg(null);
    mRepo.setInfoMessage(null);
  }

  @NonNull
  public String getHighlight() {
    return highlight;
  }

  public void setHighlight(@Nullable String highlight) {
    highlight = (highlight == null) ? "" : highlight;
    this.highlight = highlight;
  }

  @NonNull
  public LiveData<ClipEntity> getClip() {
    return clip;
  }

  @Nullable
  public ClipEntity getClipSync() {
    return clip.getValue();
  }

  public void setClip(ClipEntity clipEntity) {
    if (!ClipEntity.isWhitespace(clipEntity)) {
      Log.logD(TAG, "setting clip: " + clipEntity.toString());
      if (clipSource != null) {
        clip.removeSource(clipSource);
      }
      clipSource = mRepo.loadClip(clipEntity.getId());
      clip.addSource(clipSource, clip::setValue);
    }
  }
}
