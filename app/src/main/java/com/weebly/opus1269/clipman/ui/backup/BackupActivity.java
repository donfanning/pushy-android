/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.backup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
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
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveResourceClient;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backup.Backup;
import com.weebly.opus1269.clipman.backup.BackupFile;
import com.weebly.opus1269.clipman.backup.DriveHelper;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

public class BackupActivity extends BaseActivity {

  /** Request code for granting Drive scope */
  private final int RC_DRIVE_SUCCESS = 10;

  /** The Array of {@link BackupFile} objects */
  // TODO save restore
  private List<BackupFile> mFiles = new ArrayList<>(0);

  /** Adapter being used to display the list's data */
  private BackupAdapter mAdapter = null;

  /** Receiver to be notified of changes */
  private BroadcastReceiver mReceiver = null;

  /** Info. message if list is not visible */
  private String mInfoMessage = "";

  /** Handles high-level drive functions like sync */
  private DriveClient mDriveClient;

  /** Handle access to Drive resources/files. */
  private DriveResourceClient mDriveResourceClient;


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

    setupBackupBroadcastReceiver();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Register mReceiver to receive Backup notifications.
    //TODO add FILTER
    LocalBroadcastManager.getInstance(this)
      .registerReceiver(mReceiver, new IntentFilter(Intents.FILTER_DEVICES));

    // show list or info. message
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

    checkPermissions();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    final int id = item.getItemId();
    switch (id) {
      case R.id.action_backup:
        Backup.INST(this).doBackup(this);
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
          initializeDriveClient();
        }
        break;
      default:
        break;
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Unregister since the activity is not visible
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
  }

  /** Request Drive access if needed */
  private void checkPermissions() {
    if (!GoogleSignIn.hasPermissions(getAccount(), Drive.SCOPE_APPFOLDER)) {
      GoogleSignIn.requestPermissions(this, RC_DRIVE_SUCCESS,
        getAccount(), Drive.SCOPE_APPFOLDER);
    } else {
      initializeDriveClient();
    }
  }

  /** Initialize the Drive clients with the current user's account */
  private void initializeDriveClient() {
    mDriveClient = Drive.getDriveClient(getApplicationContext(), getAccount());
    mDriveResourceClient =
      Drive.getDriveResourceClient(getApplicationContext(), getAccount());
    onDriveClientReady();
  }

  /** Drive can be called */
  private void onDriveClientReady() {
    retrieveBackups();
  }

  /** Get last signed in account */
  private GoogleSignInAccount getAccount() {
    return GoogleSignIn.getLastSignedInAccount(this);
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
    final FloatingActionButton fab = findViewById(R.id.fab);

    if (mFiles.isEmpty()) {
      mInfoMessage = getString(R.string.err_no_backups);
    } else {
      mInfoMessage = "";
    }

    if (TextUtils.isEmpty(mInfoMessage)) {
      textView.setVisibility(View.GONE);
      recyclerView.setVisibility(View.VISIBLE);
      fab.setVisibility(View.VISIBLE);
      textView.setText("");
    } else {
      textView.setText(mInfoMessage);
      textView.setVisibility(View.VISIBLE);
      recyclerView.setVisibility(View.GONE);
      fab.setVisibility(View.GONE);
    }
  }

  /** Create the {@link BroadcastReceiver} to handle changes to the list */
  private void setupBackupBroadcastReceiver() {
    // handler for received Intents for the "backup file" event
    mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        final Bundle bundle = intent.getBundleExtra(Intents.BUNDLE_DEVICES);
        if (bundle == null) {
          return;
        }
        final String action = bundle.getString(Intents.ACTION_TYPE_DEVICES);
        if (action == null) {
          return;
        }

        switch (action) {
          case Intents.TYPE_UPDATE_DEVICES:
            // device list changed - don't ping here
            if (Devices.INST(getApplicationContext()).getCount() > 0) {
              mInfoMessage = "";
            }
            setupMainView();
            break;
          case Intents.TYPE_NO_REMOTE_DEVICES:
            // detected no remote devices - don't ping here
            mInfoMessage = getString(R.string.err_no_remote_devices);
            setupMainView();
            break;
          default:
            break;
        }
      }
    };
  }

  public DriveClient getDriveClient() {
    return mDriveClient;
  }

  public DriveResourceClient getDriveResourceClient() {
    return mDriveResourceClient;
  }

  /**
   * Get the list of backups
   * @return the backups
   */
  public List<BackupFile> getFiles() {
    return mFiles;
  }

  /** Set the list of backups */
  public void setFiles(@NonNull final ArrayList<BackupFile> files) {
    mFiles = files;
    setupMainView();
    mAdapter.notifyDataSetChanged();
  }

  /**
   * Add a flle to the list
   * @param file file to add
   */
  public void addFileToList(@NonNull final BackupFile file) {
    mFiles.add(file);
    setupMainView();
    mAdapter.notifyDataSetChanged();
  }

  /**
   * Remove a flle from the list
   * @param file file to remove
   */
  public void removeFileFromList(@NonNull final BackupFile file) {
    mFiles.remove(file);
    setupMainView();
    mAdapter.notifyDataSetChanged();
  }

  /** Delete a backup file asynchronously */
  void deleteBackup(BackupFile backupFile) {
    DriveHelper.INST(this).deleteBackupFile(this, backupFile);
  }

  /** Load the list of backup files asynchronously */
  private void retrieveBackups() {
    DriveHelper.INST(this).retrieveBackupFiles(this);
  }

  /** Refresh the list */
  private void refreshList() {
    retrieveBackups();
  }

  /** Display progress*/
  public void showProgress() {
    final View contentView = findViewById(R.id.drive_content);
    final View progressView = findViewById(R.id.drive_progress);

    contentView.setVisibility(View.GONE);
    progressView.setVisibility(View.VISIBLE);
  }

  /** Remove progress */
  public void dismissProgress() {
    final View contentView = findViewById(R.id.drive_content);
    final View progressView = findViewById(R.id.drive_progress);

    contentView.setVisibility(View.VISIBLE);
    progressView.setVisibility(View.GONE);
  }
}
