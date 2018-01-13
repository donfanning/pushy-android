/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.app.AppUtils;

/** Singleton for sending emails */
public enum Email {
  INST;

  /** Support email address */
  private static final String SUPPORT_ADDRESS = "pushyclipboard@gmail.com";

  /**
   * Get system info. for body of support requests
   * @return Email body
   */
  public String getBody(Context context) {
    return "Pushy Clipboard Version: " + BuildConfig.VERSION_NAME + '\n' +
      "Android Version: " + Build.VERSION.RELEASE + '\n' +
      "Device: " + MyDevice.INST(context).getModel() + " \n \n \n";
  }

  /**
   * Send an email to support address
   * @param ctxt    A Context
   * @param subject Email subject
   * @param body    Email body
   */
  public void send(Context ctxt, String subject, String body) {
    final Intent intent = new Intent(Intent.ACTION_SENDTO);
    intent.setData(Uri.parse("mailto:" + SUPPORT_ADDRESS));
    if (!TextUtils.isEmpty(subject)) {
      intent.putExtra(Intent.EXTRA_SUBJECT, subject);
    }
    if (!TextUtils.isEmpty(body)) {
      intent.putExtra(Intent.EXTRA_TEXT, body);
    }
    AppUtils.startNewTaskActivity(ctxt, intent);
  }
}
