/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.widget.Button;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.repos.MainRepo;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;

/** Handlers for UI events */
public class ClipHandlers extends BaseHandlers
  implements DialogInterface.OnClickListener {
  private final BaseActivity mActivity;
  private final String TAG;
  private ClipEntity mClipEntity;

  ClipHandlers(BaseActivity baseActivity) {
    super();
    this.mActivity = baseActivity;
    this.TAG = baseActivity.getTAG();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    final Button button = ((AlertDialog) dialog).getButton(which);
    final String btnText = button.getText().toString();

    Analytics.INST(button.getContext()).buttonClick(TAG, button);

    if (mActivity.getString(R.string.button_delete).equals(btnText)) {
      Log.logD(TAG, "delete clicked");
      MainRepo.INST(App.INST()).removeClipAsync(mClipEntity);
    }
  }

  /**
   * Click on delete button
   * @param context     A context
   * @param clipEntity The Label
   */
  public void onDeleteClick(Context context, ClipEntity clipEntity) {
    Log.logD(TAG, "delete clicked");
    Analytics.INST(context).imageClick(TAG, "deleteLabel");
    mClipEntity = clipEntity;
    showDialog(R.string.label_delete_dialog_title,
      R.string.label_delete_dialog_message, R.string.button_delete);
  }

  /**
   * Display a confirmation dialog
   * @param titleId  resource id of dialog title
   * @param msgId    resource id of dialog message
   * @param buttonId resource id of dialog positive button
   */
  private void showDialog(int titleId, int msgId, int buttonId) {
    final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
      .setMessage(msgId)
      .setTitle(titleId)
      .setPositiveButton(buttonId, this)
      .setNegativeButton(R.string.button_cancel, null)
      .create();

    alertDialog.show();
  }
}
