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

import com.weebly.opus1269.clipman.db.entity.BackupEntity;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.repos.BackupRepo;

import java.util.List;

/** ViewModel for BackupEntitys */
public class BackupsViewModel extends AndroidViewModel {
  /** Error message */
  private final MediatorLiveData<ErrorMsg> errorMsg;

  /** Info message */
  private final MediatorLiveData<String> infoMessage;

  /** True if loading */
  private final MediatorLiveData<Boolean> isLoading;

  /** BackFile list */
  private final MediatorLiveData<List<BackupEntity>> backupList;

  public BackupsViewModel(@NonNull Application app) {
    super(app);

    BackupRepo repo = BackupRepo.INST(app);

    errorMsg = new MediatorLiveData<>();
    errorMsg.setValue(repo.getErrorMsg().getValue());
    errorMsg.addSource(repo.getErrorMsg(), errorMsg::setValue);

    infoMessage = new MediatorLiveData<>();
    infoMessage.setValue(repo.getInfoMessage().getValue());
    infoMessage.addSource(repo.getInfoMessage(), infoMessage::setValue);

    isLoading = new MediatorLiveData<>();
    isLoading.setValue(repo.getIsLoading().getValue());
    isLoading.addSource(repo.getIsLoading(), isLoading::setValue);

    backupList = new MediatorLiveData<>();
    backupList.setValue(repo.loadBackups().getValue());
    backupList.addSource(repo.loadBackups(), backupList::setValue);
  }

  @NonNull
  public LiveData<ErrorMsg> getErrorMsg() {
    return errorMsg;
  }

  @NonNull
  public LiveData<String> getInfoMessage() {
    return infoMessage;
  }

  public LiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public LiveData<List<BackupEntity>> loadBackups() {
    return backupList;
  }
}
