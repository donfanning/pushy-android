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
import android.arch.lifecycle.MutableLiveData;
import android.databinding.Bindable;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backup.BackupFile;

import java.util.ArrayList;
import java.util.List;

/** ViewModel for BackupFiles */
public class BackupsViewModel extends AndroidViewModel {
  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** BackFile list */
  public final MutableLiveData<List<BackupFile>> files;

  /** Info message */
  public final MutableLiveData<String> infoMessage;

  /** True if loading */
  public final MutableLiveData<Boolean> isLoading;

  /** True if loading */
  //public boolean isLoading;

  public BackupsViewModel(@NonNull Application app) {
    super(app);

    files = new MutableLiveData<>();
    files.setValue(new ArrayList<>());

    infoMessage = new MutableLiveData<>();
    infoMessage.setValue("");

    isLoading = new MutableLiveData<>();
    isLoading.setValue(false);
  }

  public void postIsLoading(boolean value) {
    isLoading.postValue(value);
  }

  public void setIsLoading(boolean value) {
    isLoading.setValue(value);
  }

  public void postInfoMessage(String value) {
    infoMessage.postValue(value);
  }

  public void setInfoMessage(String value) {
    infoMessage.setValue(value);
  }

  public void refreshList() {
    //TODO
    Log.logD(TAG, "refreshed list called");
  }
}
