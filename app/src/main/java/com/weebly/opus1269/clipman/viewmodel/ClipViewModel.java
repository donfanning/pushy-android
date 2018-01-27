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
import com.weebly.opus1269.clipman.model.MyDevice;
import com.weebly.opus1269.clipman.repos.MainRepo;

import org.threeten.bp.Instant;

/** ViewModel for a {@link ClipEntity} */
public class ClipViewModel extends AndroidViewModel {
  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Our Repo */
  private final MainRepo mRepo;

  /** Our working status */
  private final MediatorLiveData<Boolean> isLoading;

  /** Our Clip */
  private final MutableLiveData<ClipEntity> clip;

  /** Our text */
  private final MutableLiveData<String> text;

  /** Original text of the clip */
  private final MutableLiveData<String> originalText;

  public final boolean addMode;

  public ClipViewModel(@NonNull Application app, @NonNull ClipEntity clip, boolean addMode) {
    super(app);
    mRepo = MainRepo.INST(app);
    mRepo.setErrorMsg(null);

    isLoading = new MediatorLiveData<>();
    isLoading.addSource(mRepo.getIsLoading(), this.isLoading::setValue);

    this.addMode = addMode;

    this.clip = new MutableLiveData<>();
    this.clip.setValue(clip);

    this.text = new MutableLiveData<>();
    this.text.setValue(clip.getText());

    this.originalText = new MutableLiveData<>();
    this.originalText.setValue(clip.getText());

    this.clip.observeForever((clipEntity) -> {
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
    if ((clipEntity == null) || (text == null) || (text.getValue() == null) ||
      (originalText.getValue() == null)) {
      mRepo.setErrorMsg(new ErrorMsg("no clip or text"));
      return;
    }

    final Context context = getApplication();
    final String oldText = originalText.getValue();
    final String newText = text.getValue();
    Log.logD(TAG, "text: " + newText);
    if (oldText.equals(newText)) {
      mRepo.setErrorMsg(new ErrorMsg("no changes"));
    }

    // save new or changed
    clipEntity.setText(context, newText);
    clipEntity.setRemote(false);
    clipEntity.setDevice(MyDevice.INST(context).getDisplayName());
    clipEntity.setDate(Instant.now().toEpochMilli());

    MainRepo.INST(App.INST()).addClipIfNewAndCopyAsync(clipEntity);
  }

  //public void setText(String text) {
  //  mRepo.updateClipTextAsync(text, originalText.getValue());
  //}

  private void setOriginalText(String originalText) {
    this.originalText.setValue(originalText);
  }
}
