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
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
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
  public void getBackups(@NonNull final BackupActivity activity) {
    final String errMessage = mContext.getString(R.string.err_get_backups);
    final DriveClient driveClient = getDriveClient();
    if (driveClient == null) {
      Log.logE(mContext, TAG, errMessage, true);
      return;
    }
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      Log.logE(mContext, TAG, errMessage, true);
      return;
    }

    activity.showProgress();
    // sync with drive
    driveClient.requestSync()
      .continueWithTask(new Continuation<Void, Task<DriveFolder>>() {
        @Override
        public Task<DriveFolder> then(@NonNull Task<Void> task)
          throws Exception {
          // get app folder
          return resourceClient.getAppFolder();
        }
      })
      .continueWithTask(new Continuation<DriveFolder, Task<MetadataBuffer>>() {
        @Override
        public Task<MetadataBuffer> then(@NonNull Task<DriveFolder> task)
          throws Exception {
          final DriveFolder folder = task.getResult();

          final Filter filter =
            Filters.eq(SearchableField.MIME_TYPE, "application/zip");
          final Query query = new Query.Builder().addFilter(filter).build();

          // query app folder
          return resourceClient.queryChildren(folder, query);
        }
      })
      .addOnSuccessListener(activity, new OnSuccessListener<MetadataBuffer>() {
        @Override
        public void onSuccess(MetadataBuffer metadataBuffer) {
          // populate the files list
          activity.setFiles(metadataBuffer);
          metadataBuffer.release();
          activity.hideProgress();
        }
      })
      .addOnFailureListener(activity, new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          if (ex instanceof ApiException) {
            // may have been rate limited, just use old list
            if (((ApiException) ex).getStatusCode() !=
              DriveStatusCodes.DRIVE_RATE_LIMIT_EXCEEDED) {
              Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex,
                errMessage, false);
            } else {
              Log.logD(TAG, "rate limited");
            }
          } else {
            Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, errMessage,
              false);
          }
          activity.hideProgress();
        }
      });
  }

  /**
   * Create a new backup - asynchronous
   * @param activity   activity
   * @param filename   zip filename
   * @param data       zipfile data
   * @param lastBackup encoded last backup, may not exist
   */
  void createBackup(@Nullable final BackupActivity activity,
                    final String filename, final byte[] data,
                    final String lastBackup) {
    final String errMessage = mContext.getString(R.string.err_create_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      Log.logE(mContext, TAG, errMessage, true);
      return;
    }

    final Task<DriveFolder> appFolderTask = resourceClient.getAppFolder();
    final Task<DriveContents> createContentsTask =
      resourceClient.createContents();

    if (activity != null) {
      activity.showProgress();
    }
    Tasks.whenAll(appFolderTask, createContentsTask)
      .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
        @Override
        public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
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
        }
      })
      .continueWithTask(new Continuation<DriveFile, Task<Metadata>>() {
        @Override
        public Task<Metadata> then(@NonNull Task<DriveFile> task) throws
          Exception {
          final DriveFile driveFile = task.getResult();
          // get new files metadata
          return resourceClient.getMetadata(driveFile);
        }
      })
      .continueWithTask(new Continuation<Metadata, Task<Void>>() {
        @Override
        public Task<Void> then(@NonNull Task<Metadata> task) throws Exception {
          final Metadata metadata = task.getResult();

          if (activity != null) {
            activity.addFileToList(metadata);
          }

          if (TextUtils.isEmpty(lastBackup)) {
            // no backup to delete - no big deal
            throw new Exception("OK");
          }

          final DriveId driveId = DriveId.decodeFromString(lastBackup);

          // delete old backup
          return resourceClient.delete(driveId.asDriveFile());
        }
      })
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          onDeleteSuccess(activity, DriveId.decodeFromString(lastBackup));
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          onDeleteFailure(activity, errMessage, ex);
        }
      });
  }

  /**
   * Update a backup - asynchronous
   * @param activity activity
   * @param file     existing drive file
   * @param data     zipfile data
   */
  void updateBackup(@NonNull final BackupActivity activity,
                    @NonNull final DriveFile file, final byte[] data) {
    final String errMessage = mContext.getString(R.string.err_update_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      Log.logE(mContext, TAG, errMessage, true);
      return;
    }

    activity.showProgress();
    resourceClient.openFile(file, DriveFile.MODE_WRITE_ONLY)
      .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
        @Override
        public Task<Void> then(@NonNull Task<DriveContents> task)
          throws Exception {
          final DriveContents contents = task.getResult();

          final OutputStream outputStream = contents.getOutputStream();
          //noinspection TryFinallyCanBeTryWithResources
          try {
            outputStream.write(data);
          } finally {
            outputStream.close();
          }

          return resourceClient.commitContents(contents, null);
        }
      })
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          Log.logD(TAG, "updated backup: " +
            file.getDriveId().encodeToString());
          activity.refreshList();
          activity.hideProgress();
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          onTaskFailure(activity, errMessage, ex);
        }
      });
  }

  /**
   * Get the contents of a backup
   * @param activity activity
   * @param file     file to retrieve
   */
  void getBackupContents(@NonNull final BackupActivity activity,
                         final DriveFile file) {
    final String errMessage = mContext.getString(R.string.err_get_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      Log.logE(mContext, TAG, errMessage, true);
      return;
    }

    final BackupContents backupContents = new BackupContents();

    activity.showProgress();
    resourceClient.openFile(file, DriveFile.MODE_READ_ONLY)
      .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
        @Override
        public Task<Void> then(@NonNull Task<DriveContents> task)
          throws Exception {
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
        }
      })
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          Log.logD(TAG, "restored backup: " +
            file.getDriveId().encodeToString());
          activity.onGetBackupContentsComplete(file, backupContents);
          activity.hideProgress();
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          onTaskFailure(activity, errMessage, ex);
        }
      });
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
      Log.logE(mContext, TAG, errMessage, true);
      return;
    }

    final DriveFile file = driveId.asDriveFile();
    final Task<Void> deleteTask = resourceClient.delete(file);

    if (activity != null) {
      activity.showProgress();
    }
    deleteTask
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          onDeleteSuccess(activity, driveId);
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          onDeleteFailure(activity, errMessage, ex);
        }
      });
  }

  /**
   * After deletion success
   * @param activity - activity
   * @param driveId  - deleted id
   */
  private void onDeleteSuccess(BackupActivity activity, DriveId driveId) {
    Log.logD(TAG, "deleted file: " + driveId.encodeToString());
    if (activity != null) {
      activity.removeFileFromList(driveId);
      activity.hideProgress();
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
    if (!"OK".equals(ex.getMessage())) {
      if (ex instanceof ApiException) {
        // don't even log not found errors
        if (((ApiException) ex).getStatusCode() !=
          DriveStatusCodes.DRIVE_RESOURCE_NOT_AVAILABLE) {
          Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex,
            msg, false);
        }
      } else {
        Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, msg,
          false);
      }
    }
    if (activity != null) {
      activity.hideProgress();
    }
  }

  /**
   * Generic Task failure
   * @param activity - activity
   * @param msg      - error message
   * @param ex       - causing exception
   */
  private void onTaskFailure(BackupActivity activity, String msg,
                             Exception ex) {
    Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, msg,
      true);
    if (activity != null) {
      activity.hideProgress();
    }
  }

  @Nullable
  private DriveClient getDriveClient() {
    DriveClient ret = null;
    final GoogleSignInAccount account = User.INST(mContext)
      .getGoogleAccount();
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
