/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.BackupEntity;
import com.weebly.opus1269.clipman.repos.BackupRepo;

import java.util.List;

/** ViewModel for BackupEntitys */
public class BackupsViewModel extends BaseRepoViewModel<BackupRepo> {
  /** BackFile list */
  private final MediatorLiveData<List<BackupEntity>> backupList;

  public BackupsViewModel(@NonNull Application app) {
    super(app, BackupRepo.INST(app));

    backupList = new MediatorLiveData<>();
    backupList.setValue(null);
    LiveData<List<BackupEntity>> backups = mRepo.loadBackups();
    backupList.addSource(backups, backupList::setValue);
  }

  public LiveData<List<BackupEntity>> loadBackups() {
    return backupList;
  }
}
