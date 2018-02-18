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
import com.weebly.opus1269.clipman.app.ClipboardHelper;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.repos.MainRepo;

import org.threeten.bp.Instant;

/** ViewModel for an editable {@link Clip} */
public class ClipEditorViewModel extends BaseRepoViewModel<MainRepo> {
  /** Our editable Clip text */
  @NonNull
  private final MutableLiveData<String> text;

  /** True if creating new {@link Clip} */
  private boolean addMode;

  /** Our Clip */
  @NonNull
  private Clip clip;

  public ClipEditorViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    addMode = true;

    clip = new Clip();

    text = new MutableLiveData<>();
    text.setValue(clip.getText());
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setInfoMessage(null);
    mRepo.setErrorMsg(null);
  }

  public boolean getAddMode() {
    return addMode;
  }

  public void setAddMode(boolean addMode) {
    this.addMode = addMode;
  }

  public void setClip(@NonNull Clip clip) {
    this.clip = clip;
    text.setValue(clip.getText());
  }

  @NonNull
  public MutableLiveData<String> getText() {
    return text;
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

    // update clip values
    clip.setText(newText);
    clip.setRemote(false);
    clip.setDate(Instant.now().toEpochMilli());
    // add filter label if it is set
    final Label label = mRepo.getFilterLabel().getValue();
    if (!Label.isWhitespace(label)) {
      clip.addLabel(label);
    }

    // persist clip
    if (addMode) {
      mRepo.addClipIfNew(this.clip, false);
    } else {
      mRepo.updateClip(this.clip, false);
    }
  }

  /** Copy clip to clipboard */
  public void copyToClipboard() {
    ClipboardHelper.copyToClipboard(getApplication(), clip);
  }

  /** Is clip in a savable state */
  public boolean cantSave() {
    final String newText = text.getValue();
    return AppUtils.isWhitespace(newText) ||
      TextUtils.equals(newText, clip.getText());
  }
}
