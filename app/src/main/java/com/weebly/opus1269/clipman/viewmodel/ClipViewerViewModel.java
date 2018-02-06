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

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

import org.threeten.bp.Instant;

/** ViewModel for a {@link ClipEntity} */
public class ClipViewerViewModel extends BaseRepoViewModel<MainRepo> {
  /** Clip from last delete operation */
  @Nullable
  private ClipEntity undoClip;

  /** Our Clip */
  @NonNull
  private final MediatorLiveData<ClipEntity> clip;

  /** Our query text */
  @NonNull
  private String query;

  /** Clip Source */
  private LiveData<ClipEntity> clipSource;

  public ClipViewerViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    undoClip = null;

    query = "";

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

  @Nullable
  public ClipEntity getUndoClip() {
    return undoClip;
  }

  public void setUndoClip(@Nullable ClipEntity undoClip) {
    this.undoClip = undoClip;
  }

  @NonNull
  public String getQuery() {
    return query;
  }

  public void setQuery(@Nullable String query) {
    query = (query == null) ? "" : query;
    this.query = query;
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
    if (clipSource != null) {
      clip.removeSource(clipSource);
    }
    if (ClipEntity.isWhitespace(clipEntity)) {
      // no clip
      Log.logD(TAG, "setting clip: null");
      clipSource = null;
      clip.setValue(null);
    } else {
      Log.logD(TAG, "setting clip: " + clipEntity.getId());
      clipSource = mRepo.getClip(clipEntity.getId());
      clip.addSource(clipSource, clip::setValue);
    }
  }

  /** Copy our Clip to the Clipboard */
  public void copyClip() {
    final ClipEntity clip = getClipSync();
    if (!ClipEntity.isWhitespace(clip)) {
      clip.setRemote(false);
      clip.setDate(Instant.now().toEpochMilli());
      clip.copyToClipboard(getApplication());
      setInfoMessage(getApplication().getString(R.string.clipboard_copy));
    }
  }

  /** Toggle the favorite state of our clip */
  public void toggleFavorite() {
    final ClipEntity clip = getClipSync();
    if (clip == null) {
      return;
    }

    // update database
    clip.setFav(!clip.getFav());
    mRepo.updateClipFav(clip);
  }

  /** Delete our clip */
  public void deleteClip() {
    final ClipEntity clip = getClipSync();
    if (clip != null) {
      mRepo.removeClip(clip);
      // save for undo
      setUndoClip(clip);
    }
  }

  /** Undo delete */
  public void undoDelete() {
    if (undoClip != null) {
      mRepo.addClipIfNew(undoClip, true);
    }
  }
}
