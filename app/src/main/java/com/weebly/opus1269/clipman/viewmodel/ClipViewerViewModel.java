/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** ViewModel for a {@link ClipEntity} */
public class ClipViewerViewModel extends BaseRepoViewModel<MainRepo> {
  /** Our Clip */
  @NonNull
  private final MutableLiveData<ClipEntity> clip;

  public ClipViewerViewModel(@NonNull Application app,
                             @NonNull ClipEntity clip) {
    super(app, MainRepo.INST(app));

    this.clip = new MutableLiveData<>();
    this.clip.setValue(clip);
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setErrorMsg(null);
  }

  public LiveData<ClipEntity> getClip() {
    return clip;
  }

  public void setClip(ClipEntity clip) {
    this.clip.setValue(clip);
  }

  public void resetErrorMsg() {
    mRepo.setErrorMsg(null);
  }

  public void saveClip() {
    //if ((text == null) || (text.getValue() == null) ||
    //  (originalText == null)) {
    //  mRepo.setErrorMsg(new ErrorMsg("no clip or text"));
    //  return;
    //}
    //
    //final Context context = getApplication();
    //final String newText = text.getValue();
    //Log.logD(TAG, "text: " + newText);
    //if (originalText.equals(newText)) {
    //  mRepo.setErrorMsg(new ErrorMsg("no changes"));
    //  return;
    //}
    //
    //// update clip
    //this.clip.setText(context, newText);
    //this.clip.setRemote(false);
    //this.clip.setDate(Instant.now().toEpochMilli());
    //
    //mRepo.addClipIfNewAndCopyAsync(this.clip);
  }
}
