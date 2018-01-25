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
import android.text.TextUtils;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.DriveStatusCodes;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.BackupEntity;
import com.weebly.opus1269.clipman.model.BackupContents;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.repos.BackupRepo;

import java.io.BufferedInputStream;
import java.io.OutputStream;

/** Singleton to manage interactions with Google Drive */
public class DriveHelper {
  // OK, because mAppCtxt is the global Application context
  /** Static instance */
  @SuppressLint("StaticFieldLeak")
  private static DriveHelper sInstance;

  /** Global Application Context */
  private final Context mAppCtxt;

  /** Class Indentifier */
  private final String TAG = this.getClass().getSimpleName();

  private DriveHelper(@NonNull Context context) {
    mAppCtxt = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static DriveHelper INST(@NonNull Context context) {
    synchronized (DriveHelper.class) {
      if (sInstance == null) {
        sInstance = new DriveHelper(context);
      }
      return sInstance;
    }
  }

  /** Do we have permissions for our appFolder */
  public boolean hasAppFolderPermission() {
    final GoogleSignInAccount account = User.INST(mAppCtxt).getGoogleAccount();
    return ((account != null) &&
      GoogleSignIn.hasPermissions(account, Drive.SCOPE_APPFOLDER));
  }

  /** Retrieve the metadata for all the backups in our appFolder */
  void getBackupsAsync() {
    final String errMessage = mAppCtxt.getString(R.string.err_get_backups);
    final DriveClient driveClient = getDriveClient();
    if (driveClient == null) {
      onClientError(errMessage);
      return;
    }
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return;
    }

    BackupRepo.INST(App.INST()).postIsLoading(true);
    // sync with drive
    driveClient.requestSync()
      .continueWithTask(task -> {
        // get app folder
        return resourceClient.getAppFolder();
      })
      .continueWithTask(task -> {
        final DriveFolder folder = task.getResult();

        final Filter filter =
          Filters.eq(SearchableField.MIME_TYPE, "application/zip");
        final Query query = new Query.Builder().addFilter(filter).build();

        // query app folder
        return resourceClient.queryChildren(folder, query);
      })
      .addOnSuccessListener(metadataBuffer -> {
        // populate the files list
        Log.logD(TAG, "got list of files");
        BackupRepo.INST(App.INST()).addBackups(metadataBuffer);
        metadataBuffer.release();
        BackupRepo.INST(App.INST()).postIsLoading(false);
      })
      .addOnFailureListener(ex -> {
        if (ex instanceof ApiException) {
          final int code = ((ApiException) ex).getStatusCode();
          if (code != DriveStatusCodes.DRIVE_RATE_LIMIT_EXCEEDED) {
            // don't show rate limit errors
            showMessage(errMessage, ex);
          } else {
            Log.logD(TAG, "rate limited");
          }
        } else {
          showMessage(errMessage, ex);
        }
        BackupRepo.INST(App.INST()).postIsLoading(false);
      });
  }

  /**
   * Create a new backup
   * @param filename   name of file
   * @param data       contents of file
   * @param lastBackup encoded last backup, may not exist
   */
  void createBackupAsync(final String filename, final byte[] data,
                         final String lastBackup) {
    final String errMessage = mAppCtxt.getString(R.string.err_create_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return;
    }

    final Task<DriveFolder> appFolderTask = resourceClient.getAppFolder();
    final Task<DriveContents> createContentsTask =
      resourceClient.createContents();

    BackupRepo.INST(App.INST()).postIsLoading(true);
    Tasks.whenAll(appFolderTask, createContentsTask)
      .continueWithTask(task -> {
        final DriveFolder appFolder = appFolderTask.getResult();
        final DriveContents contents = createContentsTask.getResult();

        final OutputStream outputStream = contents.getOutputStream();
        //noinspection TryFinallyCanBeTryWithResources
        try {
          outputStream.write(data);
        } finally {
          outputStream.close();
        }

        MetadataChangeSet.Builder builder = new MetadataChangeSet.Builder()
          .setTitle(filename)
          .setMimeType("application/zip");
        BackupEntity.setCustomProperties(mAppCtxt, builder);

        final MetadataChangeSet changeSet = builder.build();

        // create file including contents
        return resourceClient.createFile(appFolder, changeSet, contents);
      })
      .continueWithTask(task -> {
        final DriveFile driveFile = task.getResult();
        // get new files metadata
        return resourceClient.getMetadata(driveFile);
      })
      .continueWithTask(task -> {
        final Metadata metadata = task.getResult();

        BackupRepo.INST(App.INST()).addBackup(metadata);

        // persist to Prefs
        final String fileString = metadata.getDriveId().encodeToString();
        Prefs.INST(mAppCtxt).setLastBackup(fileString);

        if (TextUtils.isEmpty(lastBackup)) {
          // no backup to delete - no big deal
          Log.logD(TAG, "no old backup to delete");
          throw new Exception("OK");
        }

        final DriveId driveId = DriveId.decodeFromString(lastBackup);

        // delete old backup
        return resourceClient.delete(driveId.asDriveFile());
      })
      .addOnSuccessListener(aVoid -> onDeleteSuccess(
        DriveId.decodeFromString(lastBackup)))
      .addOnFailureListener(ex -> onDeleteFailure(lastBackup, errMessage, ex));
  }

  /**
   * Update a backup
   * @param file file to update
   * @param data update data
   */
  void updateBackupAsync(@NonNull final DriveFile file,
                         @NonNull final byte[] data) {
    final String errMessage = mAppCtxt.getString(R.string.err_update_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return;
    }

    BackupRepo.INST(App.INST()).postIsLoading(true);
    resourceClient.openFile(file, DriveFile.MODE_WRITE_ONLY)
      .continueWithTask(task -> {
        final DriveContents contents = task.getResult();

        final OutputStream outputStream = contents.getOutputStream();
        //noinspection TryFinallyCanBeTryWithResources
        try {
          outputStream.write(data);
        } finally {
          outputStream.close();
        }

        return resourceClient.commitContents(contents, null);
      })
      .addOnSuccessListener(aVoid -> {
        Log.logD(TAG, "updated backup");
        BackupHelper.INST(mAppCtxt).getBackupsAsync();
      })
      .addOnFailureListener(ex -> onTaskFailure(errMessage, ex));
  }

  /**
   * Get the contents of a backup
   * @param file   file to retrieve
   * @param isSync true is syncing with a backup
   */
  void getBackupContentsAsync(final DriveFile file, final boolean isSync) {
    final String errMessage = mAppCtxt.getString(R.string.err_get_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return;
    }

    final BackupContents backupContents = new BackupContents();

    BackupRepo.INST(App.INST()).postIsLoading(true);
    resourceClient.openFile(file, DriveFile.MODE_READ_ONLY)
      .continueWithTask(task -> {
        final DriveContents contents = task.getResult();

        BufferedInputStream bis = null;
        try {
          bis = new BufferedInputStream(contents.getInputStream());
          BackupHelper.INST(mAppCtxt).extractFromZipFile(bis, backupContents);
        } finally {
          if (bis != null) {
            bis.close();
          }
        }

        return resourceClient.discardContents(contents);
      })
      .addOnSuccessListener(aVoid -> {
        Log.logD(TAG, "got backup contents");
        onGetBackupCompleteAsync(file, backupContents, isSync);
      })
      .addOnFailureListener(ex -> onTaskFailure(errMessage, ex));
  }

  /**
   * Delete a backup
   * @param driveId driveId to delete
   */
  void deleteBackupAsync(@NonNull final DriveId driveId) {
    final String errMessage = mAppCtxt.getString(R.string.err_delete_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return;
    }

    final DriveFile file = driveId.asDriveFile();
    final Task<Void> deleteTask = resourceClient.delete(file);

    BackupRepo.INST(App.INST()).postIsLoading(true);
    deleteTask
      .addOnSuccessListener(aVoid -> onDeleteSuccess(driveId))
      .addOnFailureListener(
        ex -> onDeleteFailure(driveId.encodeToString(), errMessage, ex));
  }

  /**
   * Error getting Drive client
   * @param title - action type
   */
  private void onClientError(@NonNull String title) {
    final String msg = mAppCtxt.getString(R.string.err_internal_drive);
    Log.logE(mAppCtxt, TAG, msg, title, false);
    BackupRepo.INST(App.INST()).postErrorMsg(new ErrorMsg(title, msg));
  }

  /**
   * Contents of a backup has been retrieved
   * @param driveFile source file
   * @param contents  contents of backup
   * @param isSync    true if called during a backup sync operation
   */
  private void onGetBackupCompleteAsync(@NonNull DriveFile driveFile,
                                        @NonNull BackupContents contents,
                                        boolean isSync) {
    try {
      if (isSync) {
        BackupHelper.INST(mAppCtxt).syncContentsAsync(driveFile, contents);
      } else {
        BackupHelper.INST(mAppCtxt).restoreContentsAsync(contents);
      }
    } catch (Exception ex) {
      final String title = mAppCtxt.getString(R.string.err_update_db);
      final String msg = ex.getLocalizedMessage();
      Log.logEx(mAppCtxt, TAG, msg, ex, title, false);
      BackupRepo.INST(App.INST()).postErrorMsg(new ErrorMsg(title, msg));
    }
  }

  /**
   * Log exception and show message
   * @param msg message
   * @param ex  exception
   */
  private void showMessage(@NonNull String msg, @NonNull Exception ex) {
    final String exMsg = ex.getLocalizedMessage();
    Log.logEx(mAppCtxt, TAG, exMsg, ex, msg, false);
    BackupRepo.INST(App.INST()).postErrorMsg(new ErrorMsg(msg, exMsg));
  }

  /**
   * After deletion success
   * @param driveId deleted id
   */
  private void onDeleteSuccess(DriveId driveId) {
    Log.logD(TAG, "deleted file");
    BackupRepo.INST(App.INST()).removeBackup(driveId);
    BackupRepo.INST(App.INST()).postIsLoading(false);
  }

  /**
   * After deletion failure
   * @param driveIdString id we failed to delete from Drive
   * @param msg           error message
   * @param ex            causing exception
   */
  private void onDeleteFailure(String driveIdString, String msg, Exception ex) {
    if (!"OK".equals(ex.getLocalizedMessage())) {
      Log.logD(TAG, "failed to delete backup");
      if (ex instanceof ApiException) {
        // don't even log not found errors
        final int code = ((ApiException) ex).getStatusCode();
        if (code != DriveStatusCodes.DRIVE_RESOURCE_NOT_AVAILABLE) {
          showMessage(msg, ex);
        } else {
          Log.logD(TAG, "backup not found");
        }
      } else {
        showMessage(msg, ex);
      }
    }
    if (!TextUtils.isEmpty(driveIdString)) {
      // delete from database regardless
      BackupRepo.INST(App.INST())
        .removeBackup(DriveId.decodeFromString(driveIdString));
    }
    BackupRepo.INST(App.INST()).postIsLoading(false);
  }

  /**
   * Generic Task failure
   * @param msg error message
   * @param ex  causing exception
   */
  private void onTaskFailure(String msg, Exception ex) {
    showMessage(msg, ex);
    BackupRepo.INST(App.INST()).postIsLoading(false);
  }

  @Nullable
  private DriveClient getDriveClient() {
    DriveClient ret = null;
    final GoogleSignInAccount account = User.INST(mAppCtxt).getGoogleAccount();
    if (account != null) {
      ret = Drive.getDriveClient(mAppCtxt.getApplicationContext(), account);
    }
    return ret;
  }

  @Nullable
  private DriveResourceClient getDriveResourceClient() {
    DriveResourceClient ret = null;
    final GoogleSignInAccount account = User.INST(mAppCtxt).getGoogleAccount();
    if (account != null) {
      ret =
        Drive.getDriveResourceClient(mAppCtxt.getApplicationContext(), account);
    }
    return ret;
  }
}
