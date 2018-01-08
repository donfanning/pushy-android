/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.backup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.SQLException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.drive.DriveFile;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;

import org.zeroturnaround.zip.ZipException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;

/** Singleton to manage Google Drive data backups */
public class BackupHelper {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static BackupHelper sInstance;

  /** Global Application Context */
  private final Context mContext;

  /** Class Indentifier */
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
      final byte[] data = BackupContents.getDBAsJSON(mContext).getBytes();
      final byte[] zipContents =
        ZipHelper.INST(mContext).createContents(BACKUP_FILNAME, data);
      if (zipContents != null) {
        final String lastBackup = Prefs.INST(mContext).getLastBackup();
        final String zipFilename = getZipFilename();
        DriveHelper.INST(mContext)
          .createBackup(activity, zipFilename, zipContents, lastBackup);
      }
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_create_backup);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Update the contents of a backup
   * @param activity The calling activity
   */
  public void doUpdate(@NonNull BackupActivity activity,
                       @NonNull DriveFile driveFile,
                       @NonNull BackupContents contents) {
    try {
      final byte[] data = contents.getAsJSON().getBytes();
      final byte[] zipFile =
        ZipHelper.INST(mContext).createContents(BACKUP_FILNAME, data);
      if (zipFile != null) {
        DriveHelper.INST(mContext).updateBackup(activity, driveFile, zipFile);
      } else {
        throw new ZipException(mContext.getString(R.string.err_create_zip));
      }
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_update_backup);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Perform a restore
   * @param activity The calling activity
   * @param file     File to restore
   */
  public void doRestore(@NonNull BackupActivity activity, BackupFile file) {
    try {
      final DriveFile driveFile = file.getId().asDriveFile();
      activity.setIsSync(false);
      DriveHelper.INST(mContext).getBackupContents(activity, driveFile);
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_restore_backup);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Perform a sync
   * @param activity The calling activity
   * @param file     File to sync
   */
  public void doSync(@NonNull BackupActivity activity, BackupFile file) {
    try {
      final DriveFile driveFile = file.getId().asDriveFile();
      activity.setIsSync(true);
      DriveHelper.INST(mContext).getBackupContents(activity, driveFile);
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_sync_backup);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Perform a delete
   * @param activity The calling activity
   * @param file     File to delete
   */
  public void doDelete(@NonNull BackupActivity activity, BackupFile file) {
    try {
      DriveHelper.INST(mContext).deleteBackup(activity, file.getId());
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_delete_backup);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Replace the database with the restored data
   * @param contents database data to restore
   * @throws IOException if no contents
   * @throws SQLException if database update failed
   */
  public void saveContentsToDB(
    @Nullable BackupContents contents) throws IOException, SQLException {
    if (contents == null) {
      throw new IOException(mContext.getString(R.string.err_no_contents));
    }

    // replace database
    replaceDB(contents);
  }

  /**
   * Replace the database with a merge of the restored data
   * @param contents database data to restore
   * @throws IOException if no contents
   * @throws SQLException if database update failed
   */
  public void saveMergedContentsToDB(
    @Nullable BackupContents contents) throws IOException, SQLException {
    if (contents == null) {
      throw new IOException(mContext.getString(R.string.err_no_contents));
    }

    final BackupContents dbContents = BackupContents.getDB(mContext);
    dbContents.merge(mContext, contents);

    // replace database
    replaceDB(dbContents);
  }

  /**
   * Extract the data from a ZipFile
   * @param bis      stream of data
   * @param contents will contain the contents
   */
  void extractFromZipFile(@NonNull BufferedInputStream bis,
                          @NonNull BackupContents contents) {
    final byte[] data =
      ZipHelper.INST(mContext).extractContents(BACKUP_FILNAME, bis);
    if (data != null) {
      final BackupContents zipContents = BackupContents.get(data);
      contents.setLabels(zipContents.getLabels());
      contents.setClipItems(zipContents.getClipItems());
    }
  }

  /**
   * Replace the contents of the database
   * @param contents new contents
   * @throws SQLException - if database update failed
   */
  private void replaceDB(@NonNull BackupContents contents) throws SQLException {
    // replace database contents
    final List<Label> labels = contents.getLabels();
    final List<ClipItem> clipItems = contents.getClipItems();
    App.getDbHelper().replaceDB(labels, clipItems);

    // reset label filter if it was deleted
    final String labelfilter = Prefs.INST(mContext).getLabelFilter();
    if (!TextUtils.isEmpty(labelfilter) &&
      !LabelTables.INST(mContext).exists(labelfilter)) {
      Prefs.INST(mContext).setLabelFilter("");
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

  /**
   * Log exception and show message
   * @param activity - activity
   * @param msg      - message
   * @param ex       exception
   */
  private void showMessage(BackupActivity activity, @NonNull String msg,
                           @NonNull Exception ex) {
    final String exMsg = ex.getLocalizedMessage();
    Log.logEx(mContext, TAG, exMsg, ex, msg, false);
    if (activity != null) {
      activity.showMessage(msg, exMsg);
    }
  }
}
