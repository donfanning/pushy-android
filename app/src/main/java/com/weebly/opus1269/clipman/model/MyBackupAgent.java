/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.os.ParcelFileDescriptor;

import com.weebly.opus1269.clipman.app.Log;

import java.io.IOException;

/** Handle app backup and restore */
public class MyBackupAgent extends BackupAgent {
  private static final String TAG = "MyBackupAgent";

  public MyBackupAgent() {
  }

  @Override
  public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                       ParcelFileDescriptor newState) throws IOException {
    // only used by key/value backup
  }

  @Override
  public void onRestore(BackupDataInput data, int appVersionCode,
                        ParcelFileDescriptor newState) throws IOException {
    // only used by key/value restore
  }

  @Override
  public void onFullBackup(FullBackupDataOutput data) throws IOException {
    super.onFullBackup(data);
    Log.logD(TAG, "onFullBackup called");
  }

  @Override
  public void onQuotaExceeded(long backupDataBytes, long quotaBytes) {
    super.onQuotaExceeded(backupDataBytes, quotaBytes);
    Log.logE(this, TAG, "onQuotaExceeded", false);
  }

  @Override
  public void onRestoreFinished() {
    super.onRestoreFinished();

    final int versionCode = Prefs.INST(this).getVersionCode();
    if (versionCode <= 222001) {
      Log.logD(TAG, "converting User preferences");
      // switch to standalone prefs file for User info.
      User.INST(this).convertPrefs();
    }

    // reset device and log in info.
    Prefs.INST(this).setSN();
    Prefs.INST(this).setDeviceRegistered(false);
    User.INST(this).clear();

    Log.logD(TAG, "restore finished");
  }
}
