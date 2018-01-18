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
import com.weebly.opus1269.clipman.app.CustomAsyncTask;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.MyDevice;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;

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
  public void createBackup(@Nullable BackupActivity activity) {
    try {
      final String lastBackup = Prefs.INST(mContext).getLastBackup();
      final String zipName = getZipFilename();
      final byte[] zipData =
        BackupHelper.INST(mContext).createZipFileContentsFromDB();
      DriveHelper.INST(mContext).createBackup(activity, zipName, zipData,
        lastBackup);
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_create_backup);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Perform a delete
   * @param activity The calling activity
   * @param file     File to delete
   */
  private void deleteBackup(@NonNull BackupActivity activity, BackupFile file) {
    try {
      DriveHelper.INST(mContext).deleteBackup(activity, file.getId());
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_delete_backup);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Get the contents of a backup
   * @param activity The calling activity
   * @param file     File to restore
   */
  private void getBackupContents(@NonNull BackupActivity activity, BackupFile file,
                         boolean isSync) {
    try {
      final DriveFile driveFile = file.getId().asDriveFile();
      DriveHelper.INST(mContext).getBackupContents(activity, driveFile, isSync);
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_no_contents);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Perform a restore
   * @param activity The calling activity
   * @param contents contents to restore
   */
  private void restoreBackup(@NonNull BackupActivity activity, BackupContents
    contents) {
    try {
      BackupHelper.INST(activity).saveContentsToDB(contents);
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_restore_backup);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Perform a sync
   * @param activity The calling activity
   * @param contents contents to restore
   */
  private void syncBackup(@NonNull BackupActivity activity, DriveFile driveFile,
                  BackupContents contents) {
    try {
      final BackupContents mergedContents =
        BackupHelper.INST(activity).saveMergedContentsToDB(contents);
      final byte[] mergedData = mergedContents.getAsJSON().getBytes();
      final byte[] data =
        BackupHelper.INST(mContext).createZipFileContents(mergedData);
      DriveHelper.INST(mContext).updateBackup(activity, driveFile, data);
    } catch (Exception ex) {
      final String errMessage = mContext.getString(R.string.err_sync_backup);
      showMessage(activity, errMessage, ex);
    }
  }

  /**
   * Replace the database with the restored data
   * @param contents database data to restore
   * @throws IOException  if no contents
   * @throws SQLException if database update failed
   */
  private void saveContentsToDB(
    @Nullable BackupContents contents) throws IOException, SQLException {
    if (contents == null) {
      throw new IOException(mContext.getString(R.string.err_no_contents));
    }

    // replace database
    replaceDB(contents);
  }

  /**
   * Replace the database with a merge of the given data
   * @param contents data to merge with db
   * @throws IOException  if no contents
   * @throws SQLException if database update failed
   * @return merged contents
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
   * @param activity activity
   * @param msg      message
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

  /** AsyncTask to get the list of backups */
  public static class GetBackupsAsyncTask extends
    CustomAsyncTask<Void, Void, Void> {

    public GetBackupsAsyncTask(BackupActivity activity) {
      super(activity);

      activity.getViewModel().setIsLoading(true);
    }

    @Override
    protected Void doInBackground(Void... params) {
      if (mActivity != null) {
        DriveHelper.INST(mActivity).getBackups((BackupActivity) mActivity);
      }
      return null;
    }
  }

  /** AsyncTask to get the contents of a backup */
  public static class GetBackupContentsAsyncTask extends
    CustomAsyncTask<Void, Void, Void> {

    /** BackupFile to restore */
    private final BackupFile mBackupFile;
    /** true if we are synchronizing the backup with the local db */
    private final boolean mIsSync;

    public GetBackupContentsAsyncTask(BackupActivity activity,
                                      BackupFile backupFile, boolean isSync) {
      super(activity);

      activity.getViewModel().setIsLoading(true);
      mBackupFile = backupFile;
      mIsSync = isSync;
    }

    @Override
    protected Void doInBackground(Void... params) {
      if (mActivity != null) {
        BackupHelper.INST(mActivity)
          .getBackupContents((BackupActivity) mActivity, mBackupFile, mIsSync);
      }
      return null;
    }
  }

  /** AsyncTask to create a backup of our db */
  public static class CreateBackupAsyncTask extends
    CustomAsyncTask<Void, Void, Void> {

    public CreateBackupAsyncTask(BackupActivity activity) {
      super(activity);

      activity.getViewModel().setIsLoading(true);
    }

    @Override
    protected Void doInBackground(Void... params) {
      if (mActivity != null) {
        BackupHelper.INST(mActivity).createBackup((BackupActivity) mActivity);
      }
      return null;
    }
  }

  /** AsyncTask to restore a backup */
  public static class RestoreBackupAsyncTask extends
    CustomAsyncTask<Void, Void, Void> {
    /** New contents */
    private final BackupContents mContents;

    public RestoreBackupAsyncTask(BackupActivity activity,
                                  BackupContents contents) {
      super(activity);

      activity.getViewModel().setIsLoading(true);
      mContents = contents;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);

      if (mActivity != null) {
        ((BackupActivity) mActivity).getViewModel().setIsLoading(false);
      }
    }

    @Override
    protected Void doInBackground(Void... params) {
      if (mActivity != null) {
        BackupHelper.INST(mActivity)
          .restoreBackup((BackupActivity) mActivity, mContents);
      }
      return null;
    }
  }

  /** AsyncTask to sync a backup */
  public static class SyncBackupAsyncTask extends
    CustomAsyncTask<Void, Void, Void> {
    /** File to sync with */
    private final DriveFile mDriveFile;
    /** Contents to sync with db */
    private final BackupContents mContents;

    public SyncBackupAsyncTask(BackupActivity activity,
                               DriveFile driveFile,
                               BackupContents contents) {
      super(activity);

      activity.getViewModel().setIsLoading(true);
      mDriveFile = driveFile;
      mContents = contents;
    }

    @Override
    protected Void doInBackground(Void... params) {
      if (mActivity != null) {
        BackupHelper.INST(mActivity)
          .syncBackup((BackupActivity) mActivity, mDriveFile, mContents);
      }
      return null;
    }
  }

  /** AsyncTask to delete a backup */
  public static class DeleteBackupAsyncTask extends
    CustomAsyncTask<Void, Void, Void> {
    /** File to delete */
    private final BackupFile mBackupFile;

    public DeleteBackupAsyncTask(BackupActivity activity,
                                 BackupFile backupFile) {
      super(activity);

      activity.getViewModel().setIsLoading(true);
      mBackupFile = backupFile;
    }

    @Override
    protected Void doInBackground(Void... params) {
      if (mActivity != null) {
        BackupHelper.INST(mActivity)
          .deleteBackup((BackupActivity) mActivity, mBackupFile);
      }
      return null;
    }
  }
}
