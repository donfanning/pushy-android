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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
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
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.BackupEntity;
import com.weebly.opus1269.clipman.model.BackupContents;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.repos.BackupRepo;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Singleton to manage interactions with Google Drive */
public class DriveHelper {
  // OK, because mAppCtxt is the global Application context
  /** Static instance */
  @SuppressLint("StaticFieldLeak")
  private static DriveHelper sInstance;

  /** Global Application Context */
  private final Context mAppCtxt;

  /** Mime type of backups */
  private final String MIME_TYPE = "application/zip";

  private final String ERR_EXECUTION;

  /** InterruptedException err */
  private final String ERR_INTERRUPTED;

  /** Class Indentifier */
  private final String TAG = this.getClass().getSimpleName();

  private DriveHelper(@NonNull Context context) {
    mAppCtxt = context.getApplicationContext();
    ERR_INTERRUPTED = mAppCtxt.getString(R.string.err_interrupted_drive);
    ERR_EXECUTION = mAppCtxt.getString(R.string.err_execution_drive);
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
  List<BackupEntity> getBackups() {
    final List<BackupEntity> backups = new ArrayList<>();
    final String errMessage = mAppCtxt.getString(R.string.err_get_backups);

    final DriveClient driveClient = getDriveClient();
    if (driveClient == null) {
      onClientError(errMessage);
      return backups;
    }
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return backups;
    }

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

    try {
      MetadataBuffer metadataBuffer = Tasks.await(getBackups);
      Log.logD(TAG, "got list of files");
      for (Metadata metadata : metadataBuffer) {
        final BackupEntity backup = new BackupEntity(mAppCtxt, metadata);
        backups.add(backup);
      }
      metadataBuffer.release();
    } catch (ExecutionException ex) {
      showMessage(errMessage, ex);
    } catch (InterruptedException ex) {
      showMessage(ERR_INTERRUPTED, ex);
    }

    return backups;
  }

  /**
   * Create a new backup
   * @param filename   name of file
   * @param data       contents of file
   * @return new backup
   */
  BackupEntity createBackup(final String filename, final byte[] data) {
    BackupEntity backup = null;
    final String errMessage = mAppCtxt.getString(R.string.err_create_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return null;
    }

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


    try {
      final Metadata metadata = Tasks.await(createBackup);
      backup = new BackupEntity(mAppCtxt, metadata);
      Log.logD(TAG, "created backup");
    } catch (ExecutionException ex) {
      showMessage(errMessage, ex);
    } catch (InterruptedException ex) {
      showMessage(ERR_INTERRUPTED, ex);
    }

    return backup;
  }

  /**
   * Update a backup
   * @param file file to update
   * @param data update data
   */
  void updateBackup(@NonNull final DriveFile file,
                    @NonNull final byte[] data) {
    final String errMessage = mAppCtxt.getString(R.string.err_update_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return;
    }

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

    try {
      Tasks.await(updateBackup);
      Log.logD(TAG, "updated backup");
    } catch (ExecutionException ex) {
      showMessage(errMessage, ex);
    } catch (InterruptedException ex) {
      showMessage(ERR_INTERRUPTED, ex);
    }
  }

  /**
   * Get the contents of a backup
   * @param file file to retrieve
   * @return contents of a backup, null on error
   */
  @Nullable
  BackupContents getBackupContents(final DriveFile file) {
    final BackupContents backupContents = new BackupContents();
    final String errMessage = mAppCtxt.getString(R.string.err_get_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return null;
    }

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

    try {
      Tasks.await(getBackupContents);
      Log.logD(TAG, "got contents of file");
    } catch (ExecutionException ex) {
      showMessage(errMessage, ex);
    } catch (InterruptedException ex) {
      showMessage(ERR_INTERRUPTED, ex);
    }

    return backupContents;
  }

  /**
   * Delete a backup
   * @param driveId driveId to delete
   */
  void deleteBackup(@NonNull final DriveId driveId) {
    final String errMessage = mAppCtxt.getString(R.string.err_delete_backup);
    final DriveResourceClient resourceClient = getDriveResourceClient();
    if (resourceClient == null) {
      onClientError(errMessage);
      return;
    }

    final DriveFile file = driveId.asDriveFile();

    try {
      Tasks.await(resourceClient.delete(file));
      Log.logD(TAG, "deleted backup");
    } catch (ExecutionException ex) {
      showMessage(errMessage, ex);
    } catch (InterruptedException ex) {
      showMessage(ERR_INTERRUPTED, ex);
    }
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
   * Log exception and show message
   * @param msg message
   * @param ex  exception
   */
  private void showMessage(@NonNull String msg, @NonNull Exception ex) {
    final String exMsg = ex.getLocalizedMessage();
    Log.logEx(mAppCtxt, TAG, exMsg, ex, msg, false);
    BackupRepo.INST(App.INST()).postErrorMsg(new ErrorMsg(msg, exMsg));
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
