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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.BackupEntity;
import com.weebly.opus1269.clipman.model.BackupContents;
import com.weebly.opus1269.clipman.model.User;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Singleton to manage interactions with Google Drive */
public class DriveHelper {
  // OK, because mAppCtxt is the global Application context
  /** Static instance */
  @SuppressLint("StaticFieldLeak")
  private static DriveHelper sInstance;

  /** Global Application Context */
  private final Context mAppCtxt;

  /** Timeout for task completion */
  private final int WAIT_TIME_SECS = 40;

  /** Mime type of backups */
  private final String MIME_TYPE = "application/zip";

  /** No permissions for Drive */
  private final String ERR_PERMISSION;

  /** Error accessing Drive API */
  private final String ERR_INTERNAL;

  /** Class Indentifier */
  private final String TAG = this.getClass().getSimpleName();

  private DriveHelper(@NonNull Context context) {
    mAppCtxt = context.getApplicationContext();
    ERR_INTERNAL = mAppCtxt.getString(R.string.err_internal_drive);
    ERR_PERMISSION = mAppCtxt.getString(R.string.err_drive_scope_denied);
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

  /** No permission for our appFolder */
  public boolean noAppFolderPermission() {
    final GoogleSignInAccount account = User.INST(mAppCtxt).getGoogleAccount();
    return ((account == null) ||
      !GoogleSignIn.hasPermissions(account, Drive.SCOPE_APPFOLDER));
  }

  /**
   * Retrieve all the backups in our appFolder
   * @return list of backups
   */
  @NonNull
  List<BackupEntity> getBackups() throws ExecutionException,
    InterruptedException, TimeoutException, DriveException {
    final List<BackupEntity> backups = new ArrayList<>();

    final DriveClient driveClient = getDriveClient();
    final DriveResourceClient resourceClient = getDriveResourceClient();

    final Task<MetadataBuffer> getBackups;

    // sync with drive
    getBackups = driveClient.requestSync().continueWithTask(task -> {
      // get app folder
      return resourceClient.getAppFolder();
    }).continueWithTask(task -> {
      final DriveFolder folder = task.getResult();

      final Filter filter = Filters.eq(SearchableField.MIME_TYPE, MIME_TYPE);
      final Query query = new Query.Builder().addFilter(filter).build();

      // query app folder
      return resourceClient.queryChildren(folder, query);
    });

    MetadataBuffer metadataBuffer =
      Tasks.await(getBackups, WAIT_TIME_SECS, TimeUnit.SECONDS);
    Log.logD(TAG, "got list of backups");
    for (Metadata metadata : metadataBuffer) {
      final BackupEntity backup = new BackupEntity(mAppCtxt, metadata);
      backups.add(backup);
    }
    metadataBuffer.release();

    return backups;
  }

  /**
   * Create a new backup
   * @param filename name of file
   * @param data     contents of file
   * @return new backup
   */
  @NonNull
  BackupEntity createBackup(final String filename, final byte[] data)
    throws ExecutionException, InterruptedException, TimeoutException,
    DriveException {
    BackupEntity backup;
    final DriveResourceClient resourceClient = getDriveResourceClient();

    final Task<DriveFolder> appFolderTask = resourceClient.getAppFolder();
    final Task<DriveContents> createContentsTask =
      resourceClient.createContents();

    final Task<Metadata> createBackup;

    createBackup = Tasks.whenAll(appFolderTask, createContentsTask)
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
          .setMimeType(MIME_TYPE);
        BackupEntity.setCustomProperties(mAppCtxt, builder);

        final MetadataChangeSet changeSet = builder.build();

        // create file including contents
        return resourceClient.createFile(appFolder, changeSet, contents);
      })
      .continueWithTask(task -> {
        final DriveFile driveFile = task.getResult();

        // get new files metadata
        return resourceClient.getMetadata(driveFile);
      });


    final Metadata metadata =
      Tasks.await(createBackup, WAIT_TIME_SECS, TimeUnit.SECONDS);
    backup = new BackupEntity(mAppCtxt, metadata);
    Log.logD(TAG, "created backup");

    return backup;
  }

  /**
   * Update a backup
   * @param file file to update
   * @param data update data
   */
  void updateBackup(@NonNull final DriveFile file,
                    @NonNull final byte[] data) throws ExecutionException,
    InterruptedException, TimeoutException, DriveException {
    final DriveResourceClient resourceClient = getDriveResourceClient();

    final Task<Void> updateBackup;

    // open file
    updateBackup = resourceClient.openFile(file, DriveFile.MODE_WRITE_ONLY)
      .continueWithTask(task -> {
        final DriveContents contents = task.getResult();

        // write new contents
        final OutputStream outputStream = contents.getOutputStream();
        //noinspection TryFinallyCanBeTryWithResources
        try {
          outputStream.write(data);
        } finally {
          outputStream.close();
        }

        // commit new contents
        return resourceClient.commitContents(contents, null);
      });

    Tasks.await(updateBackup, WAIT_TIME_SECS, TimeUnit.SECONDS);
    Log.logD(TAG, "updated backup");
  }

  /**
   * Get the contents of a backup
   * @param file file to retrieve
   * @return contents of a backup, null on error
   */
  @NonNull
  BackupContents getBackupContents(final DriveFile file)
    throws ExecutionException, InterruptedException, TimeoutException,
    DriveException {
    final BackupContents backupContents = new BackupContents();
    final DriveResourceClient resourceClient = getDriveResourceClient();

    final Task<Void> getBackupContents;

    // open file
    getBackupContents = resourceClient.openFile(file, DriveFile.MODE_READ_ONLY)
      .continueWithTask(task -> {
        final DriveContents contents = task.getResult();

        // read file contents
        BufferedInputStream bis = null;
        try {
          bis = new BufferedInputStream(contents.getInputStream());
          BackupHelper.INST(mAppCtxt).extractFromZipFile(bis, backupContents);
        } finally {
          if (bis != null) {
            bis.close();
          }
        }

        // disacard file contents
        return resourceClient.discardContents(contents);
      });

    Tasks.await(getBackupContents, WAIT_TIME_SECS, TimeUnit.SECONDS);
    Log.logD(TAG, "got contents of file");

    return backupContents;
  }

  /**
   * Delete a backup
   * @param driveId driveId to delete
   */
  void deleteBackup(@NonNull final DriveId driveId) throws ExecutionException,
    InterruptedException, TimeoutException, DriveException {
    final DriveResourceClient resourceClient = getDriveResourceClient();

    final DriveFile file = driveId.asDriveFile();

    try {
      Tasks.await(resourceClient.delete(file), WAIT_TIME_SECS, TimeUnit.SECONDS);
      Log.logD(TAG, "deleted backup");
    } catch (ExecutionException ex) {
      final Throwable cause = ex.getCause();
      if ((cause != null) && (cause instanceof ApiException)) {
        if (((ApiException) cause).getStatusCode() ==
          DriveStatusCodes.DRIVE_RESOURCE_NOT_AVAILABLE) {
          // ignore if not found
          Log.logD(TAG, "backup not found");
        } else
          throw(ex);
      }
    }
  }

  /** Check permissions for our appFolder */
  private void checkAppFolderPermission() throws DriveException {
    if (noAppFolderPermission()) {
      throw new DriveException(ERR_PERMISSION);
    }
  }

  @NonNull
  private DriveClient getDriveClient() throws DriveException {
    checkAppFolderPermission();
    DriveClient driveClient = null;
    final GoogleSignInAccount account = User.INST(mAppCtxt).getGoogleAccount();
    if (account != null) {
      driveClient =
        Drive.getDriveClient(mAppCtxt.getApplicationContext(), account);
    }
    if (driveClient == null) {
      throw new DriveException(ERR_INTERNAL);
    }
    return driveClient;
  }

  @NonNull
  private DriveResourceClient getDriveResourceClient() throws DriveException {
    checkAppFolderPermission();
    DriveResourceClient driveResourceClient = null;
    final GoogleSignInAccount account = User.INST(mAppCtxt).getGoogleAccount();
    if (account != null) {
      driveResourceClient =
        Drive.getDriveResourceClient(mAppCtxt.getApplicationContext(), account);
    }
    if (driveResourceClient == null) {
      throw new DriveException(ERR_INTERNAL);
    }
    return driveResourceClient;
  }
}
