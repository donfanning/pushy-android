/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.repos;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.BackupDB;
import com.weebly.opus1269.clipman.db.entity.BackupEntity;
import com.weebly.opus1269.clipman.model.User;

import java.util.ArrayList;
import java.util.List;

/** Singleton - Repository for {@link BackupEntity} objects */
public class BackupRepo extends BaseRepo {
  @SuppressLint("StaticFieldLeak")
  private static BackupRepo sInstance;

  /** Database */
  private final BackupDB mDB;

  /** BackFile list */
  private final MediatorLiveData<List<BackupEntity>> backupList;

  private BackupRepo(final Application app) {
    super(app);

    mDB = BackupDB.INST(app);

    backupList = new MediatorLiveData<>();
    backupList.addSource(mDB.backupDao().getAll(), backups -> {
      if (mDB.getDatabaseCreated().getValue() != null) {
        postInfoMessage(backups);
        backupList.setValue(backups);
      }
    });
  }

  public static BackupRepo INST(final Application app) {
    if (sInstance == null) {
      synchronized (BackupRepo.class) {
        if (sInstance == null) {
          sInstance = new BackupRepo(app);
        }
      }
    }
    return sInstance;
  }

  public MutableLiveData<List<BackupEntity>> getFiles() {
    return backupList;
  }

  /**
   * Set the list of backups from Drive
   * @param metadataBuffer buffer containing list of backupList
   */
  public void addBackups(@NonNull MetadataBuffer metadataBuffer) {
    final List<BackupEntity> backups = new ArrayList<>();
    for (Metadata metadata : metadataBuffer) {
      final BackupEntity backup = new BackupEntity(mApp, metadata);
      backups.add(backup);
    }
    App.getExecutors().diskIO().execute(() -> {
      mDB.runInTransaction(() -> {
        mDB.backupDao().deleteAll();
        mDB.backupDao().insertAll(backups);
      });
    });
  }

  /**
   * Add a backup to the list
   * @param metadata file to add
   */
  public void addBackup(Metadata metadata) {
    final BackupEntity backup = new BackupEntity(mApp, metadata);
    App.getExecutors().diskIO().execute(() -> {
      mDB.backupDao().insertAll(backup);
    });
  }

  /**
   * Remove a backup from the list by DriveId
   * @param driveId id of file to remove
   */
  public void removeBackup(@NonNull final DriveId driveId) {
    App.getExecutors().diskIO().execute(() -> {
      final String driveIdInvariant = driveId.toInvariantString();
      int nRows = mDB.backupDao().delete(driveIdInvariant);
      Log.logD(TAG, "db removeBackup: " + nRows);
    });
  }

  private void postInfoMessage(List<BackupEntity> backupFiles) {
    final String msg;
    if (AppUtils.isEmpty(backupFiles)) {
      msg = mApp.getString(R.string.err_no_backups);
    } else if (!User.INST(mApp).isLoggedIn()) {
      msg = mApp.getString(R.string.err_not_signed_in);
    } else {
      msg = "";
    }
    infoMessage.postValue(msg);
  }
}
