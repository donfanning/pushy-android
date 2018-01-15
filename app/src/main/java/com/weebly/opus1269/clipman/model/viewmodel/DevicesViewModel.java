/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.NonNull;

/** View Model for Devices Activity */
public class DevicesViewModel extends AndroidViewModel {
  /** Application context */
  @SuppressLint("StaticFieldLeak")
  final private Context appContext;

  /** Class Indentifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Info message */
  private MutableLiveData<String> infoMessage = new MutableLiveData<>();

  public DevicesViewModel(@NonNull Application application) {
    super(application);
    appContext = getApplication().getApplicationContext();
  }

  public String getInfoMessage() {
    return infoMessage.getValue();
  }

  public void setInfoMessage(String msg) {
    infoMessage.setValue(msg);
  }

}
