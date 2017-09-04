/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.app;

import android.text.TextUtils;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.model.Analytics;

/**
 * Message logger Class
 */
public class Log {
  /**
   * @value
   */
  private static final String MY_APP = "MyApp ";

  /**
   * Log a debug message
   * @param tag     Class we are from
   * @param message message to log
   */
  public static void logD(String tag, String message) {
    if (BuildConfig.DEBUG) {
      android.util.Log.d(MY_APP + tag, message);
    }
  }

  /**
   * Log an error message
   * @param tag     Class we are from
   * @param message message to log
   * @return The message
   */
  public static String logE(String tag, String message) {
    android.util.Log.e(MY_APP + tag, message);
    Analytics.INSTANCE.error(message, tag);
    return message;
  }

  /**
   * Log an {@link Exception}
   * @param tag     Class we are from
   * @param message message to log
   * @param e       Exception to log
   * @return The message
   */
  public static String logEx(String tag, String message, Exception e) {
    String msg = "";
    if (!TextUtils.isEmpty(message)) {
      msg = message + ": ";
    }
    msg += e.getLocalizedMessage();
    android.util.Log.e(MY_APP + tag, msg);
    android.util.Log.e(MY_APP + tag, e.toString());
    Analytics.INSTANCE.exception(message, e);
    return msg;
  }
}
