/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.repos;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.ErrorMsg;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/** Singleton - Repository for {@link LabelEntity} objects */
public class MainRepo {
  @SuppressLint("StaticFieldLeak")
  private static MainRepo sInstance;

  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Application */
  private final Application mApp;

  /** Error message */
  private final MutableLiveData<ErrorMsg> errorMsg;

  /** True if loading */
  private final MutableLiveData<Boolean> isLoading;

  /** Label list */
  private final MutableLiveData<List<LabelEntity>> labels;

  private MainRepo(final Application app) {
    mApp = app;

    errorMsg = new MutableLiveData<>();
    errorMsg.setValue(null);

    isLoading = new MutableLiveData<>();
    isLoading.setValue(false);

    labels = new MutableLiveData<>();
    labels.setValue(new ArrayList<>());
  }

  public static MainRepo INST(final Application app) {
    if (sInstance == null) {
      synchronized (MainRepo.class) {
        if (sInstance == null) {
          sInstance = new MainRepo(app);
        }
      }
    }
    return sInstance;
  }

  public MutableLiveData<ErrorMsg> getErrorMsg() {
    return errorMsg;
  }

  public MutableLiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public MutableLiveData<List<LabelEntity>> getLabels() {
    return labels;
  }

  public void postErrorMsg(ErrorMsg value) {
    errorMsg.postValue(value);
  }

  public void postIsLoading(boolean value) {
    isLoading.postValue(value);
  }

  private void postLabels(@NonNull List<LabelEntity> labels) {
    sortLabels(labels);
    this.labels.postValue(labels);
  }

  /**
   * Sort list of labels in place - alphabetical
   * @param labels List to sort
   */
  private void sortLabels(@NonNull List<LabelEntity> labels) {
    java.util.Collections.sort(labels, Collator.getInstance());
  }
}
