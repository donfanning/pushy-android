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
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.repos.BaseRepo;

/** Base class for ViewModel's */
public abstract class BaseRepoViewModel<T extends BaseRepo>
  extends AndroidViewModel {
  /** Class identifier */
  protected final String TAG = this.getClass().getSimpleName();

  /** Repository */
  protected final T mRepo;

  /** Error message */
  private final MediatorLiveData<ErrorMsg> errorMsg;

  /** Info message */
  private final MediatorLiveData<String> infoMessage;

  /** True if performing async op */
  private final MediatorLiveData<Boolean> isWorking;

  protected BaseRepoViewModel(@NonNull Application application, T repo) {
    super(application);

    mRepo = repo;

    errorMsg = new MediatorLiveData<>();
    errorMsg.setValue(mRepo.getErrorMsg().getValue());
    errorMsg.addSource(mRepo.getErrorMsg(), errorMsg::setValue);

    infoMessage = new MediatorLiveData<>();
    infoMessage.setValue(mRepo.getInfoMessage().getValue());
    infoMessage.addSource(mRepo.getInfoMessage(), infoMessage::setValue);

    isWorking = new MediatorLiveData<>();
    isWorking.setValue(mRepo.getIsWorking().getValue());
    isWorking.addSource(mRepo.getIsWorking(), isWorking::setValue);
  }

  @NonNull
  public LiveData<ErrorMsg> getErrorMsg() {
    return errorMsg;
  }

  public void postErrorMsg(final ErrorMsg value) {
    errorMsg.postValue(value);
  }

  @NonNull
  public LiveData<String> getInfoMessage() {
    return infoMessage;
  }

  protected void postInfoMessage(final String value) {
    infoMessage.postValue(value);
  }

  @NonNull
  public LiveData<Boolean> getIsWorking() {
    return isWorking;
  }

  protected void postIsWorking(final boolean value) {
    isWorking.postValue(value);
  }
}
