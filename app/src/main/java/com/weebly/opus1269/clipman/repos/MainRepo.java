/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.repos;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.db.MainDB;
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

  /** Database */
  private final MainDB mDB;

  /** Error message */
  private final MutableLiveData<ErrorMsg> errorMsg;

  /** True if loading */
  private final MutableLiveData<Boolean> isLoading;

  /** Label list */
  private final MediatorLiveData<List<LabelEntity>> labelsList;

  private MainRepo(final Application app) {
    mApp = app;
    mDB = MainDB.INST(app);

    errorMsg = new MutableLiveData<>();
    errorMsg.setValue(null);

    isLoading = new MutableLiveData<>();
    isLoading.setValue(false);

    labelsList = new MediatorLiveData<>();
    labelsList.setValue(new ArrayList<>());
    labelsList.addSource(mDB.labelDao().getAll(), labels -> {
      if (mDB.getDatabaseCreated().getValue() != null) {
        labelsList.postValue(labels);
      }
    });
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
    return labelsList;
  }

  public void postErrorMsg(ErrorMsg value) {
    errorMsg.postValue(value);
  }

  public void postIsLoading(boolean value) {
    isLoading.postValue(value);
  }

  private void postLabels(@NonNull List<LabelEntity> labels) {
    sortLabels(labels);
    this.labelsList.postValue(labels);
  }

  public LiveData<LabelEntity> getLabelAsync(String name) {
    return MainDB.INST(mApp).labelDao().getLabel(name);
    //App.getExecutors().diskIO()
    //  .execute(() -> MainDB.INST(mApp).labelDao().getLabel(name));
  }

  public void addLabelAsync(@NonNull LabelEntity label) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).labelDao().insertAll(label));
  }

  public void updateLabelAsync(@NonNull String newName, @NonNull String oldName) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).labelDao().updateName(newName, oldName));
  }

  public void removeLabelAsync(@NonNull LabelEntity label) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).labelDao().delete(label));
  }

  /**
   * Sort list of labelsList in place - alphabetical
   * @param labels List to sort
   */
  private void sortLabels(@NonNull List<LabelEntity> labels) {
    java.util.Collections.sort(labels, Collator.getInstance());
  }
}
