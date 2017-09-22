/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.util.Log;

import com.google.gson.Gson;

import org.joda.time.DateTime;

/**
 * Immutable representing the last error/exception that occured
 */
public class LastError {

  private static final String PREF_LAST_ERROR = "prefLastError";

  private String mTag;
  private String mTitle;
  private String mMessage;
  private String mStack;
  private Long mTime;

  private LastError() {
    mTag = "";
    mTitle = "";
    mMessage = "";
    mStack = Log.getStackTraceString(new Exception());
    mTime = DateTime.now().getMillis();
  }

  public LastError(String tag, String title, String message, Exception ex) {
    init(tag, title, message, ex);
  }

  public LastError(String tag, String title, String message) {
    init(tag, title, message, new Exception());
  }

  private void init(String tag, String title, String message, Exception ex) {
    mTag = tag;
    mTitle = title;
    mMessage = message;
    mStack = Log.getStackTraceString(ex);
    mTime = DateTime.now().getMillis();

    // persist
    final Gson gson = new Gson();
    final String asString = gson.toJson(this);
    Prefs.set(PREF_LAST_ERROR, asString);
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
