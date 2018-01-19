/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.BackupFile;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.repos.BackupRepo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** ViewModel for BackupFiles */
public class BackupsViewModel extends AndroidViewModel {
  /** Data repository */
  private final BackupRepo mRepo;

  /** Info message */
  private final MediatorLiveData<String> infoMessage;

  /** True if loading */
  private final MediatorLiveData<Boolean> isLoading;

  /** BackFile list */
  private final MediatorLiveData<List<BackupFile>> files;

  public BackupsViewModel(@NonNull Application app) {
    super(app);

    mRepo = BackupRepo.INST(app);

    infoMessage = new MediatorLiveData<>();
    infoMessage.setValue(mRepo.getInfoMessage().getValue());
    infoMessage.addSource(mRepo.getInfoMessage(), infoMessage::setValue);

    isLoading = new MediatorLiveData<>();
    isLoading.setValue(mRepo.getIsLoading().getValue());
    isLoading.addSource(mRepo.getIsLoading(), isLoading::setValue);

    files = new MediatorLiveData<>();
    files.setValue(mRepo.getFiles().getValue());
    files.addSource(mRepo.getFiles(), files::setValue);
  }

  public MutableLiveData<String> getInfoMessage() {
    return infoMessage;
  }

  public MutableLiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public MutableLiveData<List<BackupFile>> getFiles() {
    return files;
  }
}
