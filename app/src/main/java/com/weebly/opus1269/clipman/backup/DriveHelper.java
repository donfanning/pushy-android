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
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
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
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.util.ArrayList;

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
  public void retrieveBackupFiles(@NonNull final BackupActivity activity) {
    final String errMessage = mContext.getString(R.string.err_get_backups);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      Log.logE(mContext, TAG, errMessage, true);
      return;
    }

    activity.showProgress();
    final Task<DriveFolder> appFolderTask = resourceClient.getAppFolder();
    appFolderTask
      .continueWithTask(new Continuation<DriveFolder, Task<MetadataBuffer>>() {
        @Override
        public Task<MetadataBuffer> then(@NonNull Task<DriveFolder> task)
          throws Exception {
          final DriveFolder folder = task.getResult();

          final Query query = new Query.Builder()
            .addFilter(
              Filters.eq(SearchableField.MIME_TYPE, "application/zip"))
            .build();

          // query app folder
          return resourceClient.queryChildren(folder, query);
        }
      })
      .addOnSuccessListener(activity, new OnSuccessListener<MetadataBuffer>() {
        @Override
        public void onSuccess(MetadataBuffer metadataBuffer) {
          // populate the files list
          final ArrayList<BackupFile> files = new ArrayList<>(0);
          for (Metadata metadata : metadataBuffer) {
            final BackupFile file = new BackupFile(mContext, metadata);
            files.add(file);
          }
          metadataBuffer.release();
          activity.setFiles(files);
          activity.hideProgress();
        }
      })
      .addOnFailureListener(activity, new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, errMessage,
            true);
          activity.hideProgress();
        }
      });
  }

  /**
   * Create a new backup - asynchronous
   * @param activity activity
   * @param filename filename
   * @param data     zipfile data
   */
  void createBackupFile(@Nullable final BackupActivity activity,
                        final String filename, final byte[] data) {
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
          final DriveFolder parent = appFolderTask.getResult();
          final DriveContents contents = createContentsTask.getResult();
          final OutputStream outputStream = contents.getOutputStream();
          //noinspection TryFinallyCanBeTryWithResources
          try {
            outputStream.write(data);
          } finally {
            outputStream.close();
          }

          final MetadataChangeSet.Builder builder =
            new MetadataChangeSet.Builder()
              .setTitle(filename)
              .setMimeType("application/zip");

          BackupFile.setCustomProperties(mContext, builder);

          final MetadataChangeSet changeSet = builder.build();

          return resourceClient.createFile(parent, changeSet, contents);
        }
      })
      .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
        @Override
        public void onSuccess(DriveFile driveFile) {
          persistBackupFile(activity, resourceClient, driveFile);
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, errMessage,
            true);
          if (activity != null) {
            activity.hideProgress();
          }
        }
      });
  }

  /**
   * Get the contents of a backup
   * @param activity activity
   * @param file     file to retrieve
   */
  void getBackupFileContents(@NonNull final BackupActivity activity,
                             final DriveFile file) {
    final String errMessage = mContext.getString(R.string.err_get_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      Log.logE(mContext, TAG, errMessage, true);
      return;
    }

    Task<DriveContents> openFileTask =
      resourceClient.openFile(file, DriveFile.MODE_READ_ONLY);

    activity.showProgress();
    openFileTask
      .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
        @Override
        public Task<Void> then(@NonNull Task<DriveContents> task)
          throws Exception {

          final DriveContents contents = task.getResult();

          byte[] bytes = new byte[]{};
          BufferedInputStream bis = null;
          try {
            bis = new BufferedInputStream(contents.getInputStream());
            bytes = BackupHelper.INST(mContext).extractFromZipFile(bis);
            Log.logD(TAG, "retrieved " + bytes.length + " bytes");
          } catch (Exception ex) {
            Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, errMessage,
              true);
            activity.hideProgress();
          } finally {
            if (bis != null) {
              bis.close();
            }
            activity.setBackupData(bytes);
          }

          return resourceClient.discardContents(contents);
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, errMessage,
            true);
          activity.hideProgress();
        }
      });
  }

  /**
   * Delete a {@link BackupFile} - asynchronous
   * @param activity Calling activity
   * @param driveId  driveId to delete
   */
  public void deleteBackupFile(@Nullable final BackupActivity activity,
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
      activity.removeFileFromList(driveId);
    }

    if (activity != null) {
      activity.showProgress();
    }
    deleteTask
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          Log.logD(TAG, "deleted fileId: " + driveId.encodeToString());
          if (activity != null) {
            activity.removeFileFromList(driveId);
            activity.hideProgress();
          }
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          // Unfortunate, but OK
          if (ex instanceof ApiException) {
            // don't even log not found errors
            if (((ApiException) ex).getStatusCode() != 1502) {
              Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, errMessage,
                false);
            }
          } else {
            Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, errMessage,
              false);
          }
          if (activity != null) {
            activity.hideProgress();
          }
        }
      });
  }

  /**
   * Persist new backup and delete old - asynchronous
   * @param activity       Calling activity
   * @param resourceClient Drive access
   * @param file           Folder to retrieve files from
   */
  private void persistBackupFile(@Nullable final BackupActivity activity,
                                 final DriveResourceClient resourceClient,
                                 final DriveFile file) {
    final String errMessage = "Failed to add to list";
    final Task<Metadata> metaDataTask = resourceClient.getMetadata(file);
    metaDataTask
      .addOnSuccessListener(new OnSuccessListener<Metadata>() {
        @Override
        public void onSuccess(Metadata metadata) {

          // persist to Prefs
          final String oldBackup = Prefs.INST(mContext).getLastBackup();
          final BackupFile file = new BackupFile(mContext, metadata);
          final String fileString = file.getId().encodeToString();
          Prefs.INST(mContext).setLastBackup(fileString);
          Log.logD(TAG, "created fileId: " + fileString);
          if (activity != null) {
            activity.addFileToList(file);
          }

          if (!TextUtils.isEmpty(oldBackup)) {
            // delete old backup
            DriveId driveId = DriveId.decodeFromString(oldBackup);
            deleteBackupFile(activity, driveId);
          } else if (activity != null) {
            activity.hideProgress();
          }
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, errMessage,
            true);
          if (activity != null) {
            activity.hideProgress();
          }
        }
      });
  }

  //@Nullable
  //private DriveClient getDriveClient() {
  //  DriveClient ret = null;
  //  final GoogleSignInAccount account = User.INST(mContext)
  // .getGoogleAccount();
  //  if (account != null) {
  //    ret = Drive.getDriveClient(mContext.getApplicationContext(), account);
  //  }
  //  return ret;
  //}

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
