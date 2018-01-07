/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.content.Context;
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
public class LastError implements Serializable {

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

  public LastError(Context ctxt, String tag, String title, String message,
                   Exception ex) {
    init(ctxt, tag, title, message, ex);
  }

  public LastError(Context ctxt, String tag, String title, String message) {
    init(ctxt, tag, title, message, null);
  }

  /**
   * Retrieve from Prefs
   * @param ctxt A Context
   * @return LastError from storage
   */
  public static LastError get(Context ctxt) {
    final String json =
      Prefs.INST(ctxt).get(PREF_LAST_ERROR, "");

    if (json.isEmpty()) {
      return new LastError();
    } else {
      final Gson gson = new Gson();
      return gson.fromJson(json, LastError.class);
    }
  }

  /**
   * Clear the stored LastError
   * @param ctxt A Context
   */
  public static void clear(Context ctxt) {
    final LastError lastError = new LastError();
    lastError.persist(ctxt);
  }

  /**
   * Determine if the saved LastError has a message
   * @param ctxt A Context
   * @return true if message is not empty
   */
  public static boolean exists(Context ctxt) {
    boolean ret = true;

    final LastError lastError = LastError.get(ctxt);
    if (TextUtils.isEmpty(lastError.getMessage())) {
      ret = false;
    }

    return ret;
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
   * Get relative time string
   * @param ctxt A Context
   * @return relative time
   */
  public CharSequence getRelativeTime(Context ctxt) {
    return AppUtils.getRelativeDisplayTime(ctxt, new DateTime(mTime));
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
   * @param ctxt    A Context
   * @param tag     source tag
   * @param title   title
   * @param message message
   * @param ex      causing exception
   */
  private void init(Context ctxt, String tag, String title, String message,
                    Exception ex) {
    mTag = tag;
    mTitle = title;
    mMessage = message;
    mTime = DateTime.now().getMillis();

    mStack = "";
    if (ex != null) {
      final String exMsg = ex.getLocalizedMessage();
      final String exTrace = Log.getStackTraceString(ex);
      if (!TextUtils.isEmpty(exMsg) && !exMsg.equals(message)) {
        mStack += exMsg + '\n';
      }
      if (!TextUtils.isEmpty(exTrace)) {
        mStack += exTrace;
      }
    }

    persist(ctxt);
  }

  /**
   * Persist to Prefs
   * @param ctxt A Context
   */
  private void persist(Context ctxt) {
    final Gson gson = new Gson();
    final String asString = gson.toJson(this);
    Prefs.INST(ctxt).set(PREF_LAST_ERROR, asString);
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
