/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.model.MyDevice;
import com.weebly.opus1269.clipman.repos.MainRepo;

import org.threeten.bp.Instant;

/** ViewModel for a {@link ClipEntity} */
public class ClipEditorViewModel extends BaseRepoViewModel<MainRepo> {
  /** Our Clip */
  @NonNull
  private final ClipEntity clip;

  /** Our Clip text */
  private final MutableLiveData<String> text;

  /** Original text of the clip */
  private final String originalText;

  /** True if creating new {@link ClipEntity} */
  public final boolean addMode;

  public ClipEditorViewModel(@NonNull Application app, @NonNull ClipEntity clip,
                             boolean addMode) {
    super(app, MainRepo.INST(app));

    mRepo.setErrorMsg(null);
    mRepo.setIsWorking(null);

    this.text = new MutableLiveData<>();
    this.text.setValue(clip.getText());

    this.addMode = addMode;

    this.originalText = clip.getText();

    this.clip = clip;
  }

  public MutableLiveData<String> getText() {
    return text;
  }

  public void resetErrorMsg() {
    mRepo.setErrorMsg(null);
  }

  public void saveClip() {
    if ((text == null) || (text.getValue() == null) ||
      (originalText == null)) {
      mRepo.setErrorMsg(new ErrorMsg("no clip or text"));
      return;
    }

    final Context context = getApplication();
    final String newText = text.getValue();
    Log.logD(TAG, "text: " + newText);
    if (originalText.equals(newText)) {
      mRepo.setErrorMsg(new ErrorMsg("no changes"));
      return;
    }

    // update clip
    this.clip.setText(context, newText);
    this.clip.setRemote(false);
    this.clip.setDevice(MyDevice.INST(context).getDisplayName());
    this.clip.setDate(Instant.now().toEpochMilli());

    mRepo.addClipIfNewAndCopyAsync(this.clip);
  }
}
