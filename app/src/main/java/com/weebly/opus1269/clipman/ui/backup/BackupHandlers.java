/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.backup;

import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.widget.Button;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.backup.BackupFile;
import com.weebly.opus1269.clipman.backup.BackupHelper;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.viewmodel.BackupsViewModel;

/** Handlers for UI events */
public class BackupHandlers implements
  DialogInterface.OnClickListener {
  /** Our activity */
  private final BackupActivity mActivity;

  /** Confirmation dialog */
  private AlertDialog mDialog;

  /** File that may be operated on */
  private BackupFile mFile;


  BackupHandlers(BackupActivity backupActivity) {
    this.mActivity = backupActivity;
  }

  @Override
  public void onClick(DialogInterface dialogInterface, int which) {
    if ((which == DialogInterface.BUTTON_POSITIVE) && (mFile != null)) {
      final Button button = mDialog.getButton(which);
      final String btnText = button.getText().toString();

      Analytics.INST(mActivity).buttonClick(mActivity.getTAG(), button);

      if (mActivity.getString(R.string.button_delete).equals(btnText)) {
        new BackupHelper.DeleteBackupAsyncTask(mActivity, mFile).executeMe();
      } else if (mActivity.getString(R.string.button_restore).equals(btnText)) {
        new BackupHelper
          .GetBackupContentsAsyncTask(mActivity, mFile, false).executeMe();
      } else if (mActivity.getString(R.string.button_sync).equals(btnText)) {
        new BackupHelper
          .GetBackupContentsAsyncTask(mActivity, mFile, true).executeMe();
      }
      mFile = null;
    }
  }

  /**
   * Click on fab button
   * @param fab The View
   */
  public void onFabClick(BackupsViewModel vm, FloatingActionButton fab) {
    if (fab != null) {
      if (vm != null) {
        vm.refreshList();
      }
      Analytics.INST(fab.getContext())
        .imageClick(mActivity.getTAG(), "refreshBackups");
    }
  }

  /**
   * Click on restore button
   * @param context The View
   * @param file The file
   */
  public void onRestoreClick(Context context, BackupFile file) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "restoreBackup");
    mFile = file;
    showDialog(R.string.backup_dialog_restore_message, R.string.button_restore);
  }

  /**
   * Click on sync button
   * @param context The View
   * @param file The file
   */
  public void onSyncClick(Context context, BackupFile file) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "syncBackup");
    mFile = file;
    showDialog(R.string.backup_dialog_sync_message, R.string.button_sync);
  }

  /**
   * Click on delete button
   * @param context The View
   * @param file The file
   */
  public void onDeleteClick(Context context, BackupFile file) {
    Analytics.INST(context).imageClick(mActivity.getTAG(), "deleteBackup");
    mFile = file;
    showDialog(R.string.backup_dialog_delete_message, R.string.button_delete);
  }

  /**
   * Display confirmation dialog on undoable actions
   * @param msgId    resource id of dialog message
   * @param buttonId resource id of dialog positive button
   */
  private void showDialog(int msgId, int buttonId) {
    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

    builder
      .setMessage(msgId)
      .setTitle(R.string.backup_dialog_title)
      .setPositiveButton(buttonId, this)
      .setNegativeButton(R.string.button_cancel, this);

    mDialog = builder.create();
    mDialog.show();
  }
}
