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
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.BackupContents;
import com.weebly.opus1269.clipman.model.BackupFile;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;

import java.io.BufferedInputStream;
import java.io.OutputStream;

/** Singleton to manage interactions with Google Drive */
public class DriveHelper {

  // OK, because mContext is the global Application context
  /** Static instance */
  @SuppressLint("StaticFieldLeak")
  private static DriveHelper sInstance;

  /** Global Application Context */
  private final Context mContext;

  /** Class Indentifier */
  private final String TAG = this.getClass().getSimpleName();

  private DriveHelper(@NonNull Context context) {
    mContext = context.getApplicationContext();
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
    final GoogleSignInAccount account = User.INST(mContext).getGoogleAccount();
    return ((account != null) &&
      GoogleSignIn.hasPermissions(account, Drive.SCOPE_APPFOLDER));
  }

  /**
   * Retrieve the metadata for all the backups in our appFolder - asynchronous
   * @param activity our activity
   */
  void getBackups(@NonNull final BackupActivity activity) {
    final String errMessage = mContext.getString(R.string.err_get_backups);
    final DriveClient driveClient = getDriveClient();
    if (driveClient == null) {
      onClientError(activity, errMessage);
      return;
    }
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(activity, errMessage);
      return;
    }

    setIsLoading(activity, true);
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
      .addOnSuccessListener(activity, metadataBuffer -> {
        // populate the files list
        Log.logD(TAG, "got list of files");
        activity.getViewModel().postFiles(metadataBuffer);
        metadataBuffer.release();
        setIsLoading(activity, false);
      })
      .addOnFailureListener(activity, ex -> {
        if (ex instanceof ApiException) {
          final int code = ((ApiException) ex).getStatusCode();
          if (code != DriveStatusCodes.DRIVE_RATE_LIMIT_EXCEEDED) {
            // don't show rate limit errors
            showMessage(activity, errMessage, ex);
          } else {
            Log.logD(TAG, "rate limited");
          }
        } else {
          showMessage(activity, errMessage, ex);
        }
        setIsLoading(activity, false);
      });
  }

  /**
   * Create a new backup - asynchronous
   * @param activity   activity
   * @param filename   name of file
   * @param data       contents of file
   * @param lastBackup encoded last backup, may not exist
   */
  void createBackup(@Nullable final BackupActivity activity,
                    final String filename, final byte[] data,
                    final String lastBackup) {
    final String errMessage = mContext.getString(R.string.err_create_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(activity, errMessage);
      return;
    }

    final Task<DriveFolder> appFolderTask = resourceClient.getAppFolder();
    final Task<DriveContents> createContentsTask =
      resourceClient.createContents();

    setIsLoading(activity, true);
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
        BackupFile.setCustomProperties(mContext, builder);

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

        if (activity != null) {
          activity.getViewModel().addFile(metadata);
        }

        // persist to Prefs
        final String fileString = metadata.getDriveId().encodeToString();
        Prefs.INST(mContext).setLastBackup(fileString);

        if (TextUtils.isEmpty(lastBackup)) {
          // no backup to delete - no big deal
          Log.logD(TAG, "no old backup to delete");
          throw new Exception("OK");
        }

        final DriveId driveId = DriveId.decodeFromString(lastBackup);

        // delete old backup
        return resourceClient.delete(driveId.asDriveFile());
      })
      .addOnSuccessListener(aVoid -> onDeleteSuccess(activity, DriveId.decodeFromString(lastBackup)))
      .addOnFailureListener(ex -> onDeleteFailure(activity, errMessage, ex));
  }

  /**
   * Update a backup - asynchronous
   * @param activity activity
   * @param file     file to update
   * @param data     update data
   */
  void updateBackup(@NonNull final BackupActivity activity,
                    @NonNull final DriveFile file,
                    @NonNull final byte[] data) {
    final String errMessage = mContext.getString(R.string.err_update_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(activity, errMessage);
      return;
    }

    setIsLoading(activity, true);
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
      .addOnSuccessListener(activity, aVoid -> {
        Log.logD(TAG, "updated backup");
        new BackupHelper.GetBackupsAsyncTask(activity).executeMe();
      })
      .addOnFailureListener(activity, ex -> onTaskFailure(activity, errMessage, ex));
  }

  /**
   * Get the contents of a backup
   * @param activity activity
   * @param file     file to retrieve
   * @param isSync   true is syncing with a backup
   */
  void getBackupContents(@NonNull final BackupActivity activity,
                         final DriveFile file, final boolean isSync) {
    final String errMessage = mContext.getString(R.string.err_get_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(activity, errMessage);
      return;
    }

    final BackupContents backupContents = new BackupContents();

    setIsLoading(activity, true);
    resourceClient.openFile(file, DriveFile.MODE_READ_ONLY)
      .continueWithTask(task -> {
        final DriveContents contents = task.getResult();

        BufferedInputStream bis = null;
        try {
          bis = new BufferedInputStream(contents.getInputStream());
          BackupHelper.INST(mContext).extractFromZipFile(bis, backupContents);
        } finally {
          if (bis != null) {
            bis.close();
          }
        }

        return resourceClient.discardContents(contents);
      })
      .addOnSuccessListener(activity, aVoid -> {
        Log.logD(TAG, "got backup contents");
        activity.onGetBackupContentsComplete(file, backupContents, isSync);
      })
      .addOnFailureListener(activity, ex -> onTaskFailure(activity, errMessage, ex));
  }

  /**
   * Delete a backup - asynchronous
   * @param activity Calling activity
   * @param driveId  driveId to delete
   */
  void deleteBackup(@Nullable final BackupActivity activity,
                    @NonNull final DriveId driveId) {
    final String errMessage = mContext.getString(R.string.err_delete_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(activity, errMessage);
      return;
    }

    final DriveFile file = driveId.asDriveFile();
    final Task<Void> deleteTask = resourceClient.delete(file);

    setIsLoading(activity, true);
    deleteTask
      .addOnSuccessListener(aVoid -> onDeleteSuccess(activity, driveId))
      .addOnFailureListener(ex -> onDeleteFailure(activity, errMessage, ex));
  }

  /**
   * Set isLoading state
   * @param activity - activity
   * @param value    - true if working
   */
  private void setIsLoading(BackupActivity activity, boolean value) {
    if (activity != null) {
      activity.getViewModel().postIsLoading(value);
    }
  }

  /**
   * Error getting Drive client
   * @param activity - activity
   * @param title    - action type
   */
  private void onClientError(BackupActivity activity, @NonNull String title) {
    final String msg = mContext.getString(R.string.err_internal_drive);
    Log.logE(mContext, TAG, msg, title, false);
    if (activity != null) {
      activity.getHandlers().showErrorMessage(msg, msg);
    }
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
      activity.getHandlers().showErrorMessage(msg, exMsg);
    }
  }

  /**
   * After deletion success
   * @param activity - activity
   * @param driveId  - deleted id
   */
  private void onDeleteSuccess(BackupActivity activity, DriveId driveId) {
    Log.logD(TAG, "deleted file");
    if (activity != null) {
      activity.getViewModel().removeFile(driveId);
      setIsLoading(activity, false);
    }
  }

  /**
   * After deletion failure
   * @param activity - activity
   * @param msg      - error message
   * @param ex       - causing exception
   */
  private void onDeleteFailure(BackupActivity activity, String msg,
                               Exception ex) {
    if (!"OK".equals(ex.getLocalizedMessage())) {
      Log.logD(TAG, "failed to delete backup");
      if (ex instanceof ApiException) {
        // don't even log not found errors
        final int code = ((ApiException) ex).getStatusCode();
        if (code != DriveStatusCodes.DRIVE_RESOURCE_NOT_AVAILABLE) {
          showMessage(activity, msg, ex);
        } else {
          Log.logD(TAG, "old backup not found");
        }
      } else {
        showMessage(activity, msg, ex);
      }
    }
    setIsLoading(activity, false);
  }

  /**
   * Generic Task failure
   * @param activity - activity
   * @param msg      - error message
   * @param ex       - causing exception
   */
  private void onTaskFailure(BackupActivity activity, String msg,
                             Exception ex) {
    showMessage(activity, msg, ex);
    setIsLoading(activity, false);
  }

  @Nullable
  private DriveClient getDriveClient() {
    DriveClient ret = null;
    final GoogleSignInAccount account = User.INST(mContext).getGoogleAccount();
    if (account != null) {
      ret = Drive.getDriveClient(mContext.getApplicationContext(), account);
    }
    return ret;
  }

  @Nullable
  private DriveResourceClient getDriveResourceClient() {
    DriveResourceClient ret = null;
    final GoogleSignInAccount account = User.INST(mContext).getGoogleAccount();
    if (account != null) {
      ret =
        Drive.getDriveResourceClient(mContext.getApplicationContext(), account);
    }
    return ret;
  }
}
