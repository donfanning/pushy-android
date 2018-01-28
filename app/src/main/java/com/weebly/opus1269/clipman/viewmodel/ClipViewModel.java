/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** ViewModel for a {@link ClipEntity} */
public class ClipViewModel extends AndroidViewModel {
  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Our Repo */
  private final MainRepo mRepo;

  /** Our Clip */
  private final MutableLiveData<ClipEntity> clip = new MutableLiveData<>();

  /** Our text */
  private final MutableLiveData<String> text = new MutableLiveData<>();

  /** Original text of our Clip */
  private final MutableLiveData<String> originalText = new MutableLiveData<>();

  public ClipViewModel(@NonNull Application app, ClipEntity theClip) {
    super(app);
    mRepo = MainRepo.INST(app);

    clip.setValue(theClip);

    text.setValue(theClip.getText());

    originalText.setValue(theClip.getText());

    clip.observeForever((clipEntity) -> {
      Log.logD(TAG, "clip changed: " + clipEntity.getText());
      setOriginalText(clipEntity.getText());
    });
  }

  public MutableLiveData<ClipEntity> getClip() {
    return clip;
  }

  public MutableLiveData<String> getText() {
    return text;
  }

  public MutableLiveData<String> getOriginalText() {
    return originalText;
  }

  public void setText(String text) {
    // TODO mRepo.updateClipAsync(text, originalText.getValue());
  }

  private void setOriginalText(String originalText) {
    this.originalText.setValue(originalText);
  }
}
