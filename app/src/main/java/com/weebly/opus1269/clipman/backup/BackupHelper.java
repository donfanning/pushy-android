/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.backup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.drive.DriveFile;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;

import java.io.BufferedInputStream;
import java.util.List;

/** Singleton to manage Google Drive data backups */
public class BackupHelper {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static BackupHelper sInstance;

  /** Global Application Context */
  private final Context mContext;

  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Name of file in the zipfile */
  private final String BACKUP_FILNAME = "backup.txt";

  private BackupHelper(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static BackupHelper INST(@NonNull Context context) {
    synchronized (BackupHelper.class) {
      if (sInstance == null) {
        sInstance = new BackupHelper(context);
      }
      return sInstance;
    }
  }

  /**
   * Perform a backup - OK to call without activity
   * @param activity The calling activity
   */
  public void doBackup(@Nullable BackupActivity activity) {
    final byte[] data = BackupContents.getDBAsJSON(mContext).getBytes();
    final byte[] zipFile =
      ZipHelper.INST(mContext).createZipFile(BACKUP_FILNAME, data);
    if (zipFile != null) {
      final String name = getZipFilename();
      DriveHelper.INST(mContext).createBackupFile(activity, name, zipFile);
    }
  }

  /**
   * Perform a restore
   * @param activity The calling activity
   * @param file     File to restore
   */
  public void doRestore(@NonNull BackupActivity activity, BackupFile file) {
    final DriveFile driveFile = file.getId().asDriveFile();
    DriveHelper.INST(mContext).getBackupFileContents(activity, driveFile,
      false);
  }

  /**
   * Perform a sync
   * @param activity The calling activity
   * @param file     File to restore
   */
  public void doSync(@NonNull BackupActivity activity, BackupFile file) {
    final DriveFile driveFile = file.getId().asDriveFile();
    DriveHelper.INST(mContext).getBackupFileContents(activity, driveFile, true);
  }

  /**
   * Replace the database with the restored data
   * @param contents database data to restore
   */
  void restoreContents(@Nullable BackupContents contents) {
    if (contents == null) {
      return;
    }

    // clear tables
    ClipTable.INST(mContext).deleteAll();
    LabelTables.INST(mContext).deleteAllLabels();

    // add contents
    final List<Label> labels = contents.getLabels();
    final List<ClipItem> clipItems = contents.getClipItems();
    LabelTables.INST(mContext).insertLabels(labels);
    ClipTable.INST(mContext).insert(clipItems);
  }

  /**
   * Sync the database with the given content
   * @param contents database data to restore
   */
  void syncContents(DriveFile driveFile, @Nullable BackupContents contents) {
    if (contents == null) {
      return;
    }

    // get merged data
    final BackupContents dbContents = BackupContents.getDB(mContext);
    final BackupContents merged = dbContents.merge(mContext, contents);

    // clear tables
    ClipTable.INST(mContext).deleteAll();
    LabelTables.INST(mContext).deleteAllLabels();

    // add contents
    final List<Label> labels = merged.getLabels();
    final List<ClipItem> clipItems = merged.getClipItems();
    LabelTables.INST(mContext).insertLabels(labels);
    ClipTable.INST(mContext).insert(clipItems);

    // TODO now back it up to orginal file
  }

  /**
   * Extract the data from a ZipFile
   * @return content of a backup
   */
  @Nullable
  BackupContents extractFromZipFile(@NonNull BufferedInputStream bis) {
    final byte[] data =
      ZipHelper.INST(mContext).extractFromZipFile(BACKUP_FILNAME, bis);
    if (data != null) {
      return BackupContents.get(data);
    } else {
      return null;
    }
  }

  /**
   * Get name of backup file
   * @return .zip filename
   */
  @NonNull
  private String getZipFilename() {
    String ret = Device.getMyOS() + Device.getMySN(mContext) + ".zip";
    ret = ret.replace(' ', '_');
    return ret;
  }
}
