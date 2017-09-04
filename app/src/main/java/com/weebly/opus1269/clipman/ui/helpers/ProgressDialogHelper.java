/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.helpers;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Helper class for managing a model progress dialog
 */
public class ProgressDialogHelper {

  private static ProgressDialog mDialog;

  private ProgressDialogHelper() {
  }

  private static void create(Context context) {
    mDialog = new ProgressDialog(context);
    mDialog.setIndeterminate(true);
    mDialog.setCancelable(false);
    mDialog.setCanceledOnTouchOutside(false);
  }

  public static void show(Context context, String msg) {
    if (mDialog == null) {
      create(context);
    }
    mDialog.setMessage(msg);
    mDialog.show();
  }

  public static void hide() {
    if ((mDialog != null) && mDialog.isShowing()) {
      mDialog.hide();
    }
  }

  public static void dismiss() {
    if (mDialog != null) {
      mDialog.dismiss();
      mDialog = null;
    }
  }
}
