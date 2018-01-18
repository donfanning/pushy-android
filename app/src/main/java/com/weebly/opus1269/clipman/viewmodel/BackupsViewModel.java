/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backup.BackupFile;
import com.weebly.opus1269.clipman.backup.BackupHelper;
import com.weebly.opus1269.clipman.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** ViewModel for BackupFiles */
public class BackupsViewModel extends AndroidViewModel {
  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** BackFile list */
  private final MutableLiveData<List<BackupFile>> files;

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

  public MutableLiveData<List<BackupFile>> getFiles() {
    return files;
  }

  /**
   * Set the list of backups
   * @param metadataBuffer - buffer containing list of files
   */
  public void postFiles(@NonNull MetadataBuffer metadataBuffer) {
    List<BackupFile> backupFiles = new ArrayList<>();
    for (Metadata metadata : metadataBuffer) {
      final BackupFile file = new BackupFile(getApplication(), metadata);
      backupFiles.add(file);
    }
    sortFiles(backupFiles);
    files.postValue(backupFiles);

    postInfoMessage(backupFiles);
  }


  /**
   * Remove a flle from the list by DriveId
   * @param driveId id of file to remove
   */
  public void removeFile(@NonNull final DriveId driveId) {
    List<BackupFile> files = this.files.getValue();
    if (files == null)  {
      return;
    }
    for (final Iterator<BackupFile> i = files.iterator(); i.hasNext(); ) {
      final BackupFile backupFile = i.next();
      if (backupFile.getId().equals(driveId)) {
        i.remove();
        Log.logD(TAG, "removed file from list");
        this.files.setValue(files);
        break;
      }
    }
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

  private void postInfoMessage(List<BackupFile> backupFiles) {
    final String msg;
    if (backupFiles.isEmpty()) {
      msg = getApplication().getString(R.string.err_no_backups);
    } else if (!User.INST(getApplication()).isLoggedIn()) {
      msg = getApplication().getString(R.string.err_not_signed_in);
    } else {
      msg = "";
    }
    postInfoMessage(msg);
  }

  /**
   *  Sort list of files in place - mine first, then by date
   *  @param backupFiles - List to sort
   */
  private void sortFiles(List<BackupFile> backupFiles) {
    // mine first, then by date
    // see: https://goo.gl/RZG4u8
    final Comparator<BackupFile> cmp = (lhs, rhs) -> {
      // mine first
      Boolean lhMine = lhs.isMine();
      Boolean rhMine = rhs.isMine();
      int mineCompare = rhMine.compareTo(lhMine);

      if (mineCompare != 0) {
        return mineCompare;
      } else {
        // newest first
        Long lhDate = lhs.getDate();
        Long rhDate = rhs.getDate();
        return rhDate.compareTo(lhDate);
      }
    };
    Collections.sort(backupFiles, cmp);
  }
}
