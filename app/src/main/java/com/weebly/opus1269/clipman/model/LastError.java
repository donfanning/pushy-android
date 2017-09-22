/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.weebly.opus1269.clipman.app.AppUtils;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Serializable;

/**
 * Immutable class representing the last error/exception that occured
 */
public class LastError  implements Serializable {

  public static final String PREF_LAST_ERROR = "prefLastError";

  private String mTag;
  private String mTitle;
  private Long mTime;
  private String mMessage;
  private String mStack;

  private LastError() {
    mTag = "";
    mTitle = "";
    mTime = DateTime.now().getMillis();
    mMessage = "";
    mStack = Log.getStackTraceString(new Exception());
  }

  public LastError(String tag, String title, String message, Exception ex) {
    init(tag, title, message, ex);
  }

  public LastError(String tag, String title, String message) {
    init(tag, title, message, new Exception());
  }

  @Override
  public String toString() {
    DateTime dt = new DateTime(mTime);
    DateTimeFormatter fmt = DateTimeFormat.forStyle("MM");
    String date = fmt.print(dt);

    return mTitle + "\n  on " +
      date + "\n\n" +
      mMessage + "\n\n" +
      mStack + '\n';
  }

  /**
   * Retrieve from Prefs
   * @return LastError from storage
   */
  public static LastError get() {
    final String json = Prefs.get(PREF_LAST_ERROR, "");

    if (json.isEmpty()) {
      return new LastError();
    } else {
      final Gson gson = new Gson();
      return gson.fromJson(json, LastError.class);
    }
  }

  /**
   * Clear the stored LastError
   * @return a LastError with no message
   */
  public static LastError clear() {
    final LastError lastError = new LastError();
    lastError.persist();
    return lastError;
  }

  /**
   * Determine if the saved LastError has a message
   * @return true if message is not empty
   */
  public static boolean exists() {
    boolean ret = true;

    final LastError lastError = LastError.get();
    if (TextUtils.isEmpty(lastError.getMessage())) {
      ret = false;
    }

    return ret;
  }

  /**
   * Get relative time string
   * @return relative time
   */
  public CharSequence getRelativeTime() {
    return AppUtils.getRelativeDisplayTime(new DateTime(mTime));
  }

  /**
   * Do we have an error message
   * @return true if message is not empty
   */
  public boolean hasMessage() {
    return !TextUtils.isEmpty(mMessage);
  }

  /**
   * Inialize members and persist
   * @param tag source tag
   * @param title title
   * @param message message
   * @param ex causing exception
   */
  private void init(String tag, String title, String message, Exception ex) {
    mTag = tag;
    mTitle = title;
    mMessage = message;
    mStack = Log.getStackTraceString(ex);
    mTime = DateTime.now().getMillis();

    persist();
  }

  /**
   * Persist to Prefs
   */
  private void persist() {
    final Gson gson = new Gson();
    final String asString = gson.toJson(this);
    Prefs.set(PREF_LAST_ERROR, asString);
  }

  public String getTag() {
    return mTag;
  }

  public String getTitle() {
    return mTitle;
  }

  public String getMessage() {
    return mMessage;
  }

  public String getStack() {
    return mStack;
  }

  public Long getTime() {
    return mTime;
  }
}
