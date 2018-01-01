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

import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
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
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;

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

  /**
   * Retrieve all the backups in our appFolder - asynchronous
   * @param activity our activity
   */
  public void retrieveBackupFiles(final BackupActivity activity) {
    final DriveResourceClient resourceClient =
      activity.getDriveResourceClient();
    final Task<DriveFolder> appFolderTask = resourceClient.getAppFolder();

    activity.showProgress();
    appFolderTask
      .addOnSuccessListener(activity,
        new OnSuccessListener<DriveFolder>() {
          @Override
          public void onSuccess(DriveFolder folder) {
            getZipFiles(activity, folder);
          }
        })
      .addOnFailureListener(activity, new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG, activity.getString(R.string.err_app_folder),
            ex, true);
          activity.dismissProgress();
        }
      });
  }


  /**
   * Create a new backup
   * @param activity activity
   * @param filename filename
   * @param data zipfile data
   */
  public void createBackupFile(final BackupActivity activity,
                               final String filename, final byte[] data) {
    final DriveResourceClient resourceClient =
      activity.getDriveResourceClient();

    final Task<DriveFolder> appFolderTask = resourceClient.getAppFolder();
    final Task<DriveContents> createContentsTask = resourceClient
      .createContents();

    activity.showProgress();
    Tasks.whenAll(appFolderTask, createContentsTask)
      .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
        @Override
        public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
          DriveFolder parent = appFolderTask.getResult();
          DriveContents contents = createContentsTask.getResult();
          OutputStream outputStream = contents.getOutputStream();
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

          return resourceClient.createFile(parent, changeSet, contents);
        }
      })
      .addOnSuccessListener(activity,
        new OnSuccessListener<DriveFile>() {
          @Override
          public void onSuccess(DriveFile driveFile) {
            final String fileId = driveFile.getDriveId().getResourceId();
            Log.logD(TAG, "fileId: " + fileId);
            // TODO add new BackupFile
            //final BackupFile backupFile = new BackupFile(mContext, driveFile);
            //activity.addFile(backupFile);
            activity.dismissProgress();
          }
        })
      .addOnFailureListener(activity, new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, true);
          activity.dismissProgress();
        }
      });
  }

  /**
   * Delete a {@link BackupFile}
   * @param activity   Calling activity
   * @param backupFile file to delete
   */
  public void deleteBackupFile(final BackupActivity activity,
                               final BackupFile backupFile) {
    final DriveResourceClient resourceClient =
      activity.getDriveResourceClient();

    activity.showProgress();
    resourceClient
      .delete(backupFile.getId().asDriveFile())
      .addOnSuccessListener(activity,
        new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {
            activity.removeFileFromList(backupFile);
            activity.dismissProgress();
          }
        })
      .addOnFailureListener(activity, new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG,
            activity.getString(R.string.err_delete_backup), ex, true);
          activity.dismissProgress();
        }
      });
  }

  /**
   * Retrieve all the zip files in a folder - asynchronous
   * @param activity Calling activity
   * @param folder   Folder to retireve files from
   */
  private void getZipFiles(final BackupActivity activity, DriveFolder folder) {
    final DriveResourceClient resourceClient =
      activity.getDriveResourceClient();
    final Query query = new Query.Builder()
      .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/zip"))
      .build();
    final Task<MetadataBuffer> queryTask =
      resourceClient.queryChildren(folder, query);
    queryTask
      .addOnSuccessListener(activity, new OnSuccessListener<MetadataBuffer>() {
        @Override
        public void onSuccess(MetadataBuffer metadataBuffer) {
          final ArrayList<BackupFile> files = new ArrayList<>(0);
          for (Metadata metadata : metadataBuffer) {
            final BackupFile file = new BackupFile(mContext, metadata);
            files.add(file);
          }
          activity.setFiles(files);
          metadataBuffer.release();
          activity.dismissProgress();
        }
      })
      .addOnFailureListener(activity, new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG, activity.getString(R.string.err_get_backups),
            ex, true);
          activity.dismissProgress();
        }
      });
  }
}
