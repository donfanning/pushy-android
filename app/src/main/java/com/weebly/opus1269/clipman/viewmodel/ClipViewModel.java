/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.repos.MainRepo;

import org.threeten.bp.Instant;

/** ViewModel for a {@link ClipEntity} */
public class ClipViewModel extends AndroidViewModel {
  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Our Repo */
  private final MainRepo mRepo;

  /** Our working status */
  private final MediatorLiveData<Boolean> isLoading = new MediatorLiveData<>();

  /** Our Clip */
  private final LiveData<ClipEntity> clip;

  /** Our text */
  private final MutableLiveData<String> text = new MutableLiveData<>();

  /** Original text of the clip */
  private final MutableLiveData<String> originalText  = new MutableLiveData<>();

  public final boolean addMode;

  public ClipViewModel(@NonNull Application app, String clipText, boolean addMode) {
    super(app);
    mRepo = MainRepo.INST(app);

    isLoading.addSource(mRepo.getIsLoading(), loading -> {
      this.isLoading.setValue(loading);
    });

    this.addMode = addMode;

    clip = mRepo.loadClip(clipText);

    text.setValue(clipText);

    originalText.setValue(clipText);

    clip.observeForever((clipEntity) -> {
      if (clipEntity != null) {
        Log.logD(TAG, "clip changed: " + clipEntity.getText());
        setOriginalText(clipEntity.getText());
      }
    });
  }

  public LiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public ErrorMsg getErrorMsg() {
    return mRepo.getErrorMsg().getValue();
  }

  public LiveData<ClipEntity> getClipLive() {
    return clip;
  }

  public ClipEntity getClip() {
    return clip.getValue();
  }

  public MutableLiveData<String> getText() {
    return text;
  }

  public MutableLiveData<String> getOriginalText() {
    return originalText;
  }

  public void saveClip() {
    final ClipEntity clipEntity = this.clip.getValue();
    if ((clipEntity == null) || (text == null)) {
      return;
    }

    final Context context = getApplication();
    final String oldText = originalText.getValue();
    final String newText = text.getValue();
    Log.logD(TAG, "text: " + newText);

    // save new or changed
    clipEntity.setText(context, newText);
    clipEntity.setRemote(false);
    clipEntity.setDate(Instant.now().toEpochMilli());
    MainRepo.INST(App.INST()).addClipIfNewAsync(clipEntity);

  }

  public void setText(String text) {
    mRepo.updateClipAsync(text, originalText.getValue());
  }

  private void setOriginalText(String originalText) {
    this.originalText.setValue(originalText);
  }
}
