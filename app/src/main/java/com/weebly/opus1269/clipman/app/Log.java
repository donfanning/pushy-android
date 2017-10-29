/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.app;

import android.text.TextUtils;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.LastError;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.Notifications;

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
   * @param title   title for LastError
   * @param notify  notify user if true
   * @return The message
   */
  public static String logE(String tag, String message, String title,
                             boolean notify) {
    String msg = "";
    if(!TextUtils.isEmpty(title)) {
      msg += title;
    }
    if(!TextUtils.isEmpty(message)) {
      msg += ": " + message;
    }

    android.util.Log.e(MY_APP + tag, msg);

    Analytics.INST.error(tag, msg);

    // save last error
    final LastError lastError = new LastError(tag, title, message);
    if (notify && Prefs.INST(App.getContext()).isNotifyError()) {
      // notify user
      Notifications.show(lastError);
    }

    return msg;
  }

  /**
   * Log an error message
   * @param tag     Class we are from
   * @param message message to log
   * @param notify  notify user if true
   * @return The message
   */
  public static String logE(String tag, String message, boolean notify) {
    final String title = App.getContext().getString(R.string.error_not_title);
    return logE(tag, message, title, notify);
  }

  /**
   * Log an error message
   * @param tag     Class we are from
   * @param message message to log
   * @param title   title for LastError
   * @return The message
   */
  public static String logE(String tag, String message, String title) {
    return logE(tag, message, title, true);
  }

  /**
   * Log an error message
   * @param tag     Class we are from
   * @param message message to log
   * @return The message
   */
  public static String logE(String tag, String message) {
    final String title = App.getContext().getString(R.string.error_not_title);
    return logE(tag, message, title, true);
  }

  /**
   * Log an {@link Exception}
   * @param tag     Class we are from
   * @param message message to log
   * @param ex      Exception to log
   * @param title   LastError title
   * @param notify  notify user if true
   * @return The message
   */
  public static String logEx(String tag, String message, Exception ex,
                              String title, Boolean notify) {
    String msg = "";
      if(!TextUtils.isEmpty(title)) {
      msg += title;
    }
    if(!TextUtils.isEmpty(message)) {
      msg += ": " + message;
    }

    Analytics.INST.exception(ex, msg);

    android.util.Log.e(MY_APP + tag, msg);
    android.util.Log.e(MY_APP + tag, ex.toString());

    // save last error
    final LastError lastError = new LastError(tag, title, message, ex);
    if (notify && Prefs.INST(App.getContext()).isNotifyError()) {
      Notifications.show(lastError);
    }

    return msg;
  }

  /**
   * Log an {@link Exception}
   * @param tag     Class we are from
   * @param message message to log
   * @param ex      Exception to log
   * @param notify  notify user if true
   * @return The message
   */
  public static String logEx(String tag, String message, Exception ex,
                             Boolean notify) {
    final String title = App.getContext().getString(R.string.error_not_title);
    return logEx(tag, message, ex, title, notify);
  }

  /**
   * Log an {@link Exception}
   * @param tag       Class we are from
   * @param message   message to log
   * @param exception Exception to log
   * @return The message
   */
  public static String logEx(String tag, String message, Exception exception) {
    final String title = App.getContext().getString(R.string.error_not_title);
    return logEx(tag, message, exception, title, true);
  }

  /**
   * Log an {@link Exception}
   * @param tag       Class we are from
   * @param message   message to log
   * @param exception Exception to log
   * @param title     LastError title
   * @return The message
   */
  public static String logEx(String tag, String message, Exception exception,
                             String title) {
    return logEx(tag, message, exception, title, true);
  }
}
