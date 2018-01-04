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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
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
    try {
      final byte[] zipFile = ZipHelper.INST(mContext)
        .createZipFile(BACKUP_FILNAME, getJSONStringData().getBytes());
      if (zipFile != null) {
        DriveHelper.INST(mContext)
          .createBackupFile(activity, getZipFilename(), zipFile);
      }
    } catch (Exception ex) {
      // noop
    }
  }

  /**
   * Perform a restore
   * @param activity The calling activity
   * @param file     File to restore
   */
  public void doRestore(@NonNull BackupActivity activity, BackupFile file) {
    final DriveFile driveFile = file.getId().asDriveFile();
    DriveHelper.INST(mContext).getBackupFileContents(activity, driveFile);
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
   * Extract the data from a ZipFile
   * @return content of a backup
   */
  @Nullable
  BackupContents extractFromZipFile(@NonNull BufferedInputStream bis) {
    final byte[] data =
      ZipHelper.INST(mContext).extractFromZipFile(BACKUP_FILNAME, bis);
    if (data != null) {
      return getDBContents(data);
    } else {
      return null;
    }
  }

  /**
   * Get the contents
   * @param data raw content
   * @return content of a backup
   */
  private BackupContents getDBContents(@NonNull byte[] data) {
    final JsonReader reader =
      new JsonReader(new InputStreamReader(new ByteArrayInputStream(data)));
    final Gson gson = new Gson();
    final Type type = new TypeToken<BackupContents>() {
    }.getType();
    return gson.fromJson(reader, type);
  }

  /**
   * Get the database data as a JSON string
   * @return Stringified data
   */
  private String getJSONStringData() {
    String ret;
    List<ClipItem> clipItems = ClipTable.INST(mContext).getAll(true, null);
    List<Label> labels = LabelTables.INST(mContext).getAllLabels();
    BackupContents contents = new BackupContents(labels, clipItems);
    // stringify it
    final Gson gson = new Gson();
    ret = gson.toJson(contents);
    return ret;
  }

  /**
   * Get name of backup file
   * @return .zip filename
   */
  private String getZipFilename() {
    String ret = Device.getMyOS() + Device.getMySN(mContext) + ".zip";
    ret = ret.replace(' ', '_');
    return ret;
  }

  /** Immutable inner class for the contents of a backup */
  public static class BackupContents {
    final private List<Label> labels;
    final private List<ClipItem> clipItems;

    BackupContents(@NonNull List<Label> labels,
                   @NonNull List<ClipItem> clipItems) {
      this.labels = labels;
      this.clipItems = clipItems;
    }

    public List<Label> getLabels() {
      return labels;
    }

    List<ClipItem> getClipItems() {
      return clipItems;
    }
  }
}
