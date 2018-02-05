/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.repos.MainRepo;

import org.threeten.bp.Instant;

/** ViewModel for an editable {@link ClipEntity} */
public class ClipEditorViewModel extends BaseRepoViewModel<MainRepo> {
  /** True if creating new {@link ClipEntity} */
  public final boolean addMode;

  /** Our Clip */
  @NonNull
  public final ClipEntity clip;

  /** Our editable Clip text */
  @NonNull
  public final MutableLiveData<String> text;

  public ClipEditorViewModel(@NonNull Application app, @NonNull ClipEntity clip,
                             boolean addMode) {
    super(app, MainRepo.INST(app));

    text = new MutableLiveData<>();
    text.setValue(clip.getText());

    this.addMode = addMode;

    this.clip = clip;
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setInfoMessage(null);
    mRepo.setErrorMsg(null);
  }

  /** Save clip to database */
  public void saveClip() {
    final String newText = text.getValue();
    if (AppUtils.isWhitespace(newText)) {
      mRepo.setErrorMsg(
        new ErrorMsg(getApplication().getString(R.string.repo_no_clip_text)));
      return;
    } else if (TextUtils.equals(newText, clip.getText())) {
      mRepo.setErrorMsg(
        new ErrorMsg(getApplication().getString(R.string.repo_same_clip_text)));
      return;
    }

    // update clip
    clip.setText(getApplication(), newText);
    clip.setRemote(false);
    clip.setDate(Instant.now().toEpochMilli());
    if (addMode) {
      mRepo.addClipIfNew(this.clip);
    } else {
      mRepo.updateClip(this.clip);
    }
  }

  /** Copy clip to clipboard */
  public void copyToClipboard() {
    clip.copyToClipboard(getApplication());
  }

  /** Is clip in a savable state */
  public boolean cantSave() {
    final String newText = text.getValue();
    return AppUtils.isWhitespace(newText) ||
      TextUtils.equals(newText, clip.getText());
  }
}
