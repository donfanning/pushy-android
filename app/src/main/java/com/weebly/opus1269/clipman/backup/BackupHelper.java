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
import com.google.android.gms.drive.DriveId;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.db.entity.BackupEntity;
import com.weebly.opus1269.clipman.model.BackupContents;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.MyDevice;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.repos.BackupRepo;

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

  /** Get the list of backups from Drive */
  public void getBackupsAsync() {
    App.getExecutors().networkIO().execute(() -> {
      BackupRepo.INST(App.INST()).postIsLoading(true);
      List<BackupEntity> backups = DriveHelper.INST(mContext).getBackups();
      BackupRepo.INST(App.INST()).addBackups(backups);
      BackupRepo.INST(App.INST()).postIsLoading(false);
    });
  }

  /** Perform a backup */
  public void createBackupAsync() {
    App.getExecutors().diskIO().execute(() -> {
      try {
        BackupRepo.INST(App.INST()).postIsLoading(true);
        final String lastBackup = Prefs.INST(mContext).getLastBackup();
        final String zipName = getZipFilename();
        final byte[] zipData =
          BackupHelper.INST(mContext).createZipFileContentsFromDB();
        DriveHelper.INST(mContext)
          .createBackupAsync(zipName, zipData, lastBackup);
      } catch (Exception ex) {
        final String errMessage =
          mContext.getString(R.string.err_create_backup);
        showMessage(errMessage, ex);
      }
    });
  }

  /**
   * Perform a backup - don't call from main thread
   * TODO anything special here?
   */
  public void createBackup() {
    try {
      final String lastBackup = Prefs.INST(mContext).getLastBackup();
      final String zipName = getZipFilename();
      final byte[] zipData =
        BackupHelper.INST(mContext).createZipFileContentsFromDB();
      DriveHelper.INST(mContext).createBackupAsync(zipName, zipData,
        lastBackup);
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_create_backup);
      showMessage(errMessage, ex);
    }
  }

  /**
   * Delete a backup
   * @param backup File to delete
   */
  public void deleteBackupAsync(@NonNull BackupEntity backup) {
    App.getExecutors().networkIO().execute(() -> {
      final DriveId driveId = backup.getDriveId();
      BackupRepo.INST(App.INST()).postIsLoading(true);
      DriveHelper.INST(mContext).deleteBackup(driveId);
      BackupRepo.INST(App.INST()).removeBackup(driveId);
      BackupRepo.INST(App.INST()).postIsLoading(false);
    });
  }

  /**
   * Get the contents of a backup
   * @param backup File to restore
   */
  public void getBackupContentsAsync(BackupEntity backup, boolean isSync) {
    try {
      final DriveFile driveFile = backup.getDriveId().asDriveFile();
      App.getExecutors().networkIO().execute(() -> DriveHelper.INST(mContext)
        .getBackupContentsAsync(driveFile, isSync));
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_no_contents);
      showMessage(errMessage, ex);
    }
  }

  /**
   * Restore the contents of a backup
   * @param contents contents to restore
   */
  public void restoreContentsAsync(final BackupContents contents) {
    App.getExecutors().diskIO().execute(() -> {
      try {
        BackupHelper.INST(mContext).saveContentsToDB(contents);
        BackupRepo.INST(App.INST()).postIsLoading(false);
      } catch (Exception ex) {
        final String errMessage =
          mContext.getString(R.string.err_restore_backup);
        showMessage(errMessage, ex);
      }
    });
  }

  /**
   * Sync the contents of a backup
   * @param contents contents to restore
   */
  public void syncContentsAsync(DriveFile driveFile,
                                BackupContents contents) {
    App.getExecutors().diskIO().execute(() -> {
      try {
        final BackupContents mergedContents =
          BackupHelper.INST(mContext).saveMergedContentsToDB(contents);
        final byte[] mergedData = mergedContents.getAsJSON().getBytes();
        final byte[] data =
          BackupHelper.INST(mContext).createZipFileContents(mergedData);
        DriveHelper.INST(mContext).updateBackupAsync(driveFile, data);
      } catch (Exception ex) {
        final String errMessage =
          mContext.getString(R.string.err_sync_backup);
        showMessage(errMessage, ex);
      }
    });
  }

  /**
   * Replace the database with the restored data
   * @param contents database data to restore
   * @throws IOException  if no contents
   * @throws SQLException if database update failed
   */
  private void saveContentsToDB(@Nullable BackupContents contents) throws
    IOException, SQLException {
    if (contents == null) {
      throw new IOException(mContext.getString(R.string.err_no_contents));
    }

    // replace database
    replaceDB(contents);
  }

  /**
   * Replace the database with a merge of the given data
   * @param contents data to merge with db
   * @return merged contents
   * @throws IOException  if no contents
   * @throws SQLException if database update failed
   */
  private BackupContents saveMergedContentsToDB(
    @Nullable BackupContents contents) throws IOException, SQLException {
    if (contents == null) {
      throw new IOException(mContext.getString(R.string.err_no_contents));
    }

    final BackupContents dbContents = BackupContents.getDB(mContext);
    dbContents.merge(mContext, contents);

    // replace database
    replaceDB(dbContents);

    return dbContents;
  }

  /**
   * Create the contents of a Zip file from the database
   * @return zip file contents
   * @throws IOException on processing data
   */
  private byte[] createZipFileContentsFromDB() throws IOException {
    final byte[] data = BackupContents.getDBAsJSON(mContext).getBytes();
    return createZipFileContents(data);
  }

  /**
   * Create the contents of a Zip file as a byte array
   * @param data contents of zip file
   * @return zip file contents
   * @throws IOException if no contents
   */
  private byte[] createZipFileContents(@NonNull byte[] data) throws
    IOException {
    return ZipHelper.INST(mContext).createContents(BACKUP_FILNAME, data);
  }

  /**
   * Extract the data from a ZipFile into a BackupContents
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
    String ret = MyDevice.INST(mContext).getOS() +
      MyDevice.INST(mContext).getSN() + ".zip";
    ret = ret.replace(' ', '_');
    return ret;
  }

  /**
   * Log exception and show message
   * @param msg message
   * @param ex  exception
   */
  private void showMessage(@NonNull String msg, @NonNull Exception ex) {
    final String exMsg = ex.getLocalizedMessage();
    Log.logEx(mContext, TAG, exMsg, ex, msg, false);
    BackupRepo.INST(App.INST()).postErrorMsg(new ErrorMsg(msg, exMsg));
  }
}
