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
import com.weebly.opus1269.clipman.db.MainDB;
import com.weebly.opus1269.clipman.db.entity.Backup;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.model.BackupContents;
import com.weebly.opus1269.clipman.model.ErrorMsg;
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
      try {
        BackupRepo.INST(App.INST()).postIsWorking(true);
        List<Backup> backups = DriveHelper.INST(mContext).getBackups();
        BackupRepo.INST(App.INST()).addBackups(backups);
      } catch (Exception ex) {
        BackupRepo.INST(App.INST()).removeAll();
        final String err = mContext.getString(R.string.err_get_backups);
        showMessage(err, ex);
      } finally {
        BackupRepo.INST(App.INST()).postIsWorking(false);
      }
    });
  }

  /** Create a backup on Drive */
  public void createBackupAsync() {
    App.getExecutors().diskIO().execute(() -> {
      try {
        BackupRepo.INST(App.INST()).postIsWorking(true);
        final String zipName = getZipFilename();
        final byte[] zipData = createZipContentsFromDB();
        final Backup backup =
          DriveHelper.INST(mContext).createBackup(zipName, zipData);
        BackupRepo.INST(App.INST()).addBackup(backup);
        final String lastBackup = Prefs.INST(mContext).getLastBackup();
        Prefs.INST(mContext).setLastBackup(backup.getDriveIdString());
        if (!TextUtils.isEmpty(lastBackup)) {
          // delete old backup
          final DriveId driveId = DriveId.decodeFromString(lastBackup);
          DriveHelper.INST(mContext).deleteBackup(driveId);
          BackupRepo.INST(App.INST()).removeBackup(driveId);
        }
      } catch (Exception ex) {
        final String err = mContext.getString(R.string.err_create_backup);
        showMessage(err, ex);
      } finally {
        BackupRepo.INST(App.INST()).postIsWorking(false);
      }
    });
  }

  /**
   * Restore the contents of a backup to the database
   * @param backup File to restore
   */
  public void restoreBackupAsync(final Backup backup) {
    App.getExecutors().networkIO().execute(() -> {
      try {
        BackupRepo.INST(App.INST()).postIsWorking(true);
        final DriveFile driveFile = backup.getDriveId().asDriveFile();
        final BackupContents contents =
          DriveHelper.INST(mContext).getBackupContents(driveFile);
        saveContentsToDB(contents);
      } catch (Exception ex) {
        final String err = mContext.getString(R.string.err_restore_backup);
        showMessage(err, ex);
      } finally {
        BackupRepo.INST(App.INST()).postIsWorking(false);
      }
    });
  }

  /**
   * Synchronize the contents of a backup and the database
   * @param backup File to sync with
   */
  public void syncContentsAsync(final Backup backup) {
    App.getExecutors().networkIO().execute(() -> {
      try {
        BackupRepo.INST(App.INST()).postIsWorking(true);
        final DriveFile driveFile = backup.getDriveId().asDriveFile();
        final BackupContents contents =
          DriveHelper.INST(mContext).getBackupContents(driveFile);
        final BackupContents mergedContents = saveMergedContentsToDB(contents);
        final byte[] mergedData = mergedContents.getAsJSON().getBytes();
        final byte[] data = createZipContents(mergedData);
        DriveHelper.INST(mContext).updateBackup(driveFile, data);
        getBackupsAsync();
      } catch (Exception ex) {
        final String err = mContext.getString(R.string.err_sync_backup);
        showMessage(err, ex);
      }
    });
  }

  /**
   * Delete a backup
   * @param backup File to delete
   */
  public void deleteBackupAsync(@NonNull Backup backup) {
    App.getExecutors().networkIO().execute(() -> {
      try {
        final DriveId driveId = backup.getDriveId();
        BackupRepo.INST(App.INST()).postIsWorking(true);
        DriveHelper.INST(mContext).deleteBackup(driveId);
        BackupRepo.INST(App.INST()).removeBackup(driveId);
        BackupRepo.INST(App.INST()).postIsWorking(false);
      } catch (Exception ex) {
        final String err = mContext.getString(R.string.err_delete_backup);
        showMessage(err, ex);
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

    final BackupContents dbContents = BackupContents.getDB();
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
  private byte[] createZipContentsFromDB() throws IOException {
    final byte[] data = BackupContents.getDBAsJSON().getBytes();
    return createZipContents(data);
  }

  /**
   * Create the contents of a Zip file as a byte array
   * @param data contents of zip file
   * @return zip file contents
   * @throws IOException if no contents
   */
  private byte[] createZipContents(@NonNull byte[] data) throws
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
      contents.setClips(zipContents.getClips());
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
    final List<Clip> clips = contents.getClips();
    MainDB.INST(App.INST()).replaceDB(labels, clips);
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
