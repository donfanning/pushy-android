/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.repos;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.weebly.opus1269.clipman.model.ErrorMsg;

/** Base class for data repositories */
public abstract class BaseRepo {
  /** Class identifier */
  protected final String TAG = this.getClass().getSimpleName();

  /** Application */
  protected final Application mApp;

  /** Error message */
  protected final MutableLiveData<ErrorMsg> errorMsg;

  /** Info message */
  protected final MutableLiveData<String> infoMessage;

  /** True if loading */
  protected final MutableLiveData<Boolean> isWorking;

  BaseRepo(final Application app) {
    mApp = app;

    errorMsg = new MutableLiveData<>();
    errorMsg.postValue(null);

    infoMessage = new MutableLiveData<>();
    infoMessage.postValue("");

    isWorking = new MutableLiveData<>();
    isWorking.postValue(false);
  }

  public LiveData<ErrorMsg> getErrorMsg() {
    return errorMsg;
  }

  public LiveData<String> getInfoMessage() {
    return infoMessage;
  }

  public LiveData<Boolean> getIsWorking() {
    return isWorking;
  }

  public void setErrorMsg(ErrorMsg value) {
    errorMsg.setValue(value);
  }

  public void setIsWorking(Boolean value) {
    isWorking.setValue(value);
  }

  public void setInfoMessage(String value) {
    infoMessage.setValue(value);
  }

  public void postErrorMsg(ErrorMsg value) {
    errorMsg.postValue(value);
  }

  public void postIsWorking(Boolean value) {
    isWorking.postValue(value);
  }

  public void postInfoMessage(String value) {
    infoMessage.postValue(value);
  }
}
