/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.base;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.weebly.opus1269.clipman.R;

/** Base class for Handlers of UI events */
public class BaseHandlers implements DialogInterface.OnClickListener {
  /** Class identifier */
  protected String TAG = this.getClass().getSimpleName();

  public BaseHandlers() {
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    // noop
  }

  /**
   * Display a confirmation dialog
   * @param context  A context
   * @param titleId  Resource id of dialog title
   * @param msgId    Resource id of dialog message
   * @param buttonId Resource id of dialog positive button
   */
  protected void showConfirmationDialog(Context context, int titleId, int msgId,
                                        int buttonId) {
    new AlertDialog.Builder(context)
      .setMessage(msgId)
      .setTitle(titleId)
      .setPositiveButton(buttonId, this)
      .setNegativeButton(R.string.button_cancel, null)
      .create()
      .show();
  }

}
