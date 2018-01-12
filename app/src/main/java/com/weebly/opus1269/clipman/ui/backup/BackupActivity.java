/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.backup;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backup.BackupContents;
import com.weebly.opus1269.clipman.backup.BackupHelper;
import com.weebly.opus1269.clipman.backup.BackupFile;
import com.weebly.opus1269.clipman.backup.DriveHelper;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.errorviewer.ErrorViewerActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class BackupActivity extends BaseActivity {
  /** Request code for granting Drive scope */
  private final int RC_DRIVE_SUCCESS = 10;

  /** The Array of {@link BackupFile} objects */
  private List<BackupFile> mFiles = new ArrayList<>(0);

  /** Adapter being used to display the list's data */
  private BackupAdapter mAdapter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_backup;

    super.onCreate(savedInstanceState);

    final FloatingActionButton fab = findViewById(R.id.fab);
    if (fab != null) {
      fab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Analytics.INST(v.getContext()).imageClick(TAG, "refreshBackups");
          refreshList();
        }
      });
    }

    setupRecyclerView();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // setup UI
    setupMainView();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mOptionsMenuID = R.menu.menu_backup;

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  protected void onStart() {
    super.onStart();

    checkDrivePermissions();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    final int id = item.getItemId();
    switch (id) {
      case R.id.action_backup:
        showConfirmDialog();
        break;
      default:
        processed = false;
        break;
    }

    if (processed) {
      Analytics.INST(this).menuClick(TAG, item);
    }

    return processed || super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resCode, Intent data) {
    super.onActivityResult(requestCode, resCode, data);

    switch (requestCode) {
      case RC_DRIVE_SUCCESS:
        if (resCode != RESULT_OK) {
          // User id not approve Google Drive permission
          Log.logE(this, TAG, getString(R.string.err_drive_scope_denied),
            false);
          finish();
          return;
        } else {
          onDriveClientReady();
        }
        break;
      default:
        break;
    }
  }

  /**
   * Get the list of backups
   * @return the backups
   */
  List<BackupFile> getFiles() {
    return mFiles;
  }

  /**
   * Set the list of backups
   * @param metadataBuffer - buffer containing list of files
   */
  public void setFiles(@NonNull MetadataBuffer metadataBuffer) {
    final ArrayList<BackupFile> files = new ArrayList<>(0);
    for (Metadata metadata : metadataBuffer) {
      final BackupFile file = new BackupFile(this, metadata);
      files.add(file);
    }
    mFiles = files;
    setupMainView();
    mAdapter.notifyDataSetChanged();
  }

  /**
   * Add a flle to the list
   * @param metadata file to add
   */
  public void addFileToList(Metadata metadata) {
    final BackupFile file = new BackupFile(this, metadata);
    // persist to Prefs
    final String fileString = file.getId().encodeToString();
    Prefs.INST(this).setLastBackup(fileString);
    Log.logD(TAG, "created fileId: " + fileString);
    boolean added = mFiles.add(file);
    if (added) {
      setupMainView();
      mAdapter.notifyDataSetChanged();
    }
  }

  /**
   * Remove a flle from the list by DriveId
   * @param driveId id of file to remove
   */
  public void removeFileFromList(@NonNull final DriveId driveId) {
    boolean found = false;
    for (final Iterator<BackupFile> i = mFiles.iterator(); i.hasNext(); ) {
      final BackupFile backupFile = i.next();
      if (backupFile.getId().equals(driveId)) {
        Log.logD(TAG, "removing from list: " + backupFile.getDate());
        found = true;
        i.remove();
        break;
      }
    }
    if (found) {
      setupMainView();
      mAdapter.notifyDataSetChanged();
    }
  }

  /**
   * Contents of a backup has been retrieved
   * @param driveFile source file
   * @param contents contents of backup
   * @param isSync true if called during a backup sync operation
   */
  public void onGetBackupContentsComplete(@NonNull DriveFile driveFile,
                                          @NonNull BackupContents contents,
                                          boolean isSync) {
    try {
      if (isSync) {
        new BackupHelper
          .SyncBackupAsyncTask(this, driveFile, contents).executeMe();
      } else {
        new BackupHelper
          .RestoreBackupAsyncTask(this, contents).executeMe();
      }
    } catch (Exception ex) {
      final String title = getString(R.string.err_update_db);
      final String msg = ex.getLocalizedMessage();
      Log.logEx(this, TAG, msg, ex, title, false);
      showMessage(title, msg);
    }
  }

  /** Refresh the list */
  public void refreshList() {
    retrieveBackups();
  }


  /** Display confirmation dialog on undoable action */
  private void showConfirmDialog() {
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder
      .setMessage(R.string.backup_dialog_backup_message)
      .setTitle(R.string.backup_dialog_title)
      .setNegativeButton(R.string.button_cancel, null)
      .setPositiveButton(R.string.button_backup, new AlertDialog
        .OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          final BackupActivity activity = BackupActivity.this;
          Analytics.INST(activity).buttonClick
            (activity.getTAG(), ((AlertDialog) dialog).getButton(which));
          new BackupHelper.CreateBackupAsyncTask(activity).executeMe();
        }
      })
      .create()
      .show();
  }

  /**
   * Display a message in a dialog
   * @param title dialog title
   * @param msg   dialog meesage
   */
  public void showMessage(@NonNull String title, @NonNull String msg) {
    hideProgress();
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder
      .setTitle(title)
      .setMessage(msg)
      .setPositiveButton(R.string.button_dismiss, null)
      .setNegativeButton(R.string.button_details, new AlertDialog
        .OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          final Intent intent = new Intent(BackupActivity.this,
            ErrorViewerActivity.class);
          AppUtils.startActivity(BackupActivity.this, intent);
        }
      });

    builder.create().show();
  }

  /** Display progress UI */
  public void showProgress() {
    final View contentView = findViewById(R.id.drive_content);
    final View fabView = findViewById(R.id.fab);
    final View progressView = findViewById(R.id.progress_layout);

    contentView.setVisibility(View.GONE);
    fabView.setVisibility(View.GONE);
    progressView.setVisibility(View.VISIBLE);
  }

  /** Hide progress UI */
  public void hideProgress() {
    final View contentView = findViewById(R.id.drive_content);
    final View fabView = findViewById(R.id.fab);
    final View progressView = findViewById(R.id.progress_layout);

    contentView.setVisibility(View.VISIBLE);
    fabView.setVisibility(View.VISIBLE);
    progressView.setVisibility(View.GONE);
  }

  /** Request Drive access if needed */
  private void checkDrivePermissions() {
    final GoogleSignInAccount account = User.INST(this).getGoogleAccount();
    if (account == null) {
      return;
    }

    if (!DriveHelper.INST(this).hasAppFolderPermission()) {
      GoogleSignIn.requestPermissions(this, RC_DRIVE_SUCCESS,
        account, Drive.SCOPE_APPFOLDER);
    } else {
      onDriveClientReady();
    }
  }

  /** Sort files - mine first, then by data */
  private void sortFiles() {
    // mine first, then by date
    // see: https://goo.gl/RZG4u8
    final Comparator<BackupFile> cmp = new Comparator<BackupFile>() {
      @Override
      public int compare(BackupFile lhs, BackupFile rhs) {
        // mine first
        Boolean lhMine = lhs.isMine();
        Boolean rhMine = rhs.isMine();
        int mineCompare = rhMine.compareTo(lhMine);

        if (mineCompare != 0) {
          return mineCompare;
        } else {
          // newest first
          Long lhDate = lhs.getDate();
          Long rhDate = rhs.getDate();
          return rhDate.compareTo(lhDate);
        }
      }
    };
    Collections.sort(mFiles, cmp);
  }

  /** Drive can be called */
  private void onDriveClientReady() {
    retrieveBackups();
  }

  /** Connect the {@link BackupAdapter} to the {@link RecyclerView} */
  private void setupRecyclerView() {
    final RecyclerView recyclerView = findViewById(R.id.backupList);
    if (recyclerView != null) {
      mAdapter = new BackupAdapter(this);
      recyclerView.setAdapter(mAdapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
  }

  /** Determine if list or info. message should be shown */
  private void setupMainView() {
    final RecyclerView recyclerView = findViewById(R.id.backupList);
    final TextView textView = findViewById(R.id.info_message);
    String infoMessage;

    if (mFiles.isEmpty()) {
      infoMessage = getString(R.string.err_no_backups);
    } else if (!User.INST(getApplicationContext()).isLoggedIn()) {
      infoMessage = getString(R.string.err_not_signed_in);
    } else {
      sortFiles();
      infoMessage = "";
    }
    textView.setText(infoMessage);

    if (TextUtils.isEmpty(infoMessage)) {
      textView.setVisibility(View.GONE);
      recyclerView.setVisibility(View.VISIBLE);
    } else {
      textView.setVisibility(View.VISIBLE);
      recyclerView.setVisibility(View.GONE);
    }
  }

  /** Load the list of backup files asynchronously */
  private void retrieveBackups() {
    new BackupHelper.GetBackupsAsyncTask(this).executeMe();
  }
}
