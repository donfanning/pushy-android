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

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backup.BackupFile;
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

  /** Info message */
  public final MutableLiveData<String> infoMessage;

  /** True if loading */
  public final MutableLiveData<Boolean> isLoading;

  /** BackFile list */
  private final MutableLiveData<List<BackupFile>> files;

  public BackupsViewModel(@NonNull Application app) {
    super(app);

    infoMessage = new MutableLiveData<>();
    infoMessage.setValue("");

    isLoading = new MutableLiveData<>();
    isLoading.setValue(false);

    files = new MutableLiveData<>();
    files.setValue(new ArrayList<>());
    files.observeForever(this::postInfoMessage);
  }

  public MutableLiveData<List<BackupFile>> getFiles() {
    return files;
  }

  /**
   * Set the list of backups from Drive
   * @param metadataBuffer - buffer containing list of files
   */
  public void postFiles(@NonNull MetadataBuffer metadataBuffer) {
    List<BackupFile> backupFiles = new ArrayList<>();
    for (Metadata metadata : metadataBuffer) {
      final BackupFile file = new BackupFile(getApplication(), metadata);
      backupFiles.add(file);
    }
    postFiles(backupFiles);
  }

  public void postIsLoading(boolean value) {
    isLoading.postValue(value);
  }

  /**
   * Add a flle to the list
   * @param metadata file to add
   */
  public void addFile(Metadata metadata) {
    if (this.files.getValue() == null)  {
      this.files.postValue(new ArrayList<>());
    }
    List<BackupFile> backupFiles = this.files.getValue();
    if (backupFiles == null)  {
      return;
    }
    final BackupFile file = new BackupFile(getApplication(), metadata);
    boolean added = backupFiles.add(file);
    if (added) {
      Log.logD(TAG, "added file to list");
      postFiles(backupFiles);
    }
  }

  /**
   * Remove a flle from the list by DriveId
   * @param driveId id of file to remove
   */
  public void removeFile(@NonNull final DriveId driveId) {
    List<BackupFile> backupFiles = this.files.getValue();
    if (backupFiles == null)  {
      return;
    }
    boolean found = false;
    for (final Iterator<BackupFile> i = backupFiles.iterator(); i.hasNext(); ) {
      final BackupFile backupFile = i.next();
      if (backupFile.getId().equals(driveId)) {
        found = true;
        i.remove();
        Log.logD(TAG, "removed file from list");
        break;
      }
    }
    if (found) {
      postFiles(backupFiles);
    }
  }

  private void postFiles(@NonNull List<BackupFile> backupFiles) {
    sortFiles(backupFiles);
    this.files.postValue(backupFiles);
  }

  private void postInfoMessage(@NonNull List<BackupFile> backupFiles) {
    final String msg;
    if (backupFiles.isEmpty()) {
      msg = getApplication().getString(R.string.err_no_backups);
    } else if (!User.INST(getApplication()).isLoggedIn()) {
      msg = getApplication().getString(R.string.err_not_signed_in);
    } else {
      msg = "";
    }
    infoMessage.postValue(msg);
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
