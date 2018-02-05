/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.backup;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.Drive;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backup.BackupHelper;
import com.weebly.opus1269.clipman.backup.DriveHelper;
import com.weebly.opus1269.clipman.databinding.BackupBinding;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.viewmodel.BackupsViewModel;

/** Manage our backups on Google Drive */
public class BackupActivity extends BaseActivity<BackupBinding> {
  /** Request code for granting Drive scope */
  private final int RC_DRIVE_SUCCESS = 10;

  /** Our event handlers */
  private BackupHandlers mHandlers = null;

  /** Adapter being used to display the list's data */
  private BackupAdapter mAdapter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_backup;
    mIsBound = true;

    super.onCreate(savedInstanceState);

    // setup ViewModel and data binding
    final BackupsViewModel vm = new BackupsViewModel(getApplication());
    mHandlers = new BackupHandlers(this);
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(vm);
    mBinding.setIsWorking(vm.getIsWorking());
    mBinding.setInfoMessage(vm.getInfoMessage());
    mBinding.setHandlers(mHandlers);
    mBinding.executePendingBindings();

    // observe errors
    vm.getErrorMsg().observe(this, errorMsg -> {
      if (errorMsg != null) {
        mHandlers.showErrorMessage(errorMsg);
      }
    });

    mAdapter = new BackupAdapter(this, mHandlers);
    mBinding.contentLayout.recycler.setAdapter(mAdapter);

    // Observe backups
    vm.getBackups().observe(this, backups -> {
      if (backups != null) {
        mAdapter.setList(backups);
      }
    });
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
        mHandlers.onBackupClick(this, item);
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
          // User did not approve Google Drive permission
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

  /** Request Drive access if needed */
  private void checkDrivePermissions() {
    final GoogleSignInAccount account = User.INST(this).getGoogleAccount();
    if (account == null) {
      return;
    }

    if (DriveHelper.INST(this).noAppFolderPermission()) {
      GoogleSignIn.requestPermissions(this, RC_DRIVE_SUCCESS,
        account, Drive.SCOPE_APPFOLDER);
    } else {
      onDriveClientReady();
    }
  }

  /** Drive can be called */
  private void onDriveClientReady() {
    BackupHelper.INST(this).getBackupsAsync();
  }

}
