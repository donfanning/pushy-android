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

import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;

import java.util.ArrayList;

/** Singleton to manage interactions with Google DriveHelper */
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

  public void retrieveBackupFiles(final BackupActivity activity) {
    final DriveResourceClient resourceClient =
      activity.getDriveResourceClient();
    final Task<DriveFolder> appFolderTask = resourceClient.getAppFolder();

    appFolderTask
      .addOnSuccessListener(activity,
        new OnSuccessListener<DriveFolder>() {
          @Override
          public void onSuccess(DriveFolder folder) {
            Log.logD(TAG, "appfolder: " + folder.toString());
            getZipFiles(activity, folder);
          }
        })
      .addOnFailureListener(activity, new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG, activity.getString(R.string.err_app_folder),
            ex, true);
        }
      });
  }

  /** Retrieve all the zip files in a folder */
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
          }
        })
      .addOnFailureListener(activity, new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception ex) {
          Log.logEx(mContext, TAG, activity.getString(R.string.err_get_backups),
            ex, true);
        }
      });
  }
}
