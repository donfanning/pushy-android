/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.repos;

import android.app.Application;
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
  protected final MutableLiveData<Boolean> isLoading;

  BaseRepo(final Application app) {
    mApp = app;

    errorMsg = new MutableLiveData<>();
    errorMsg.postValue(null);

    infoMessage = new MutableLiveData<>();
    infoMessage.postValue("");

    isLoading = new MutableLiveData<>();
    isLoading.postValue(false);
  }

  public MutableLiveData<ErrorMsg> getErrorMsg() {
    return errorMsg;
  }

  public MutableLiveData<String> getInfoMessage() {
    return infoMessage;
  }

  public MutableLiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public void setErrorMsg(ErrorMsg value) {
    errorMsg.setValue(value);
  }

  public void postErrorMsg(ErrorMsg value) {
    errorMsg.postValue(value);
  }

  public void postIsLoading(boolean value) {
    isLoading.postValue(value);
  }

  public void postInfoMessage(String value) {
    infoMessage.postValue(value);
  }
}
