/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;

/**
 * Singleton for Google Analytics tracking.
 * @see <a href="https://goo.gl/VUowF7">Android Analytics</a>
 */
public enum Analytics {
  INST;

  public static final String CAT_UI = "ui";
  public static final String UI_TOGGLE = "toggle";
  public static final String UI_LIST = "listSelect";
  public static final String UI_MULTI_LIST = "multiListSelect";
  public static final String UI_EDIT_TEXT = "editText";

  /**
   * Google Analytics tracking ID
   */
  private static final String TRACKING_ID = "UA-61314754-3";
  private static final String CAT_APP = "app";
  private static final String CAT_MSG = "message";
  private static final String CAT_REG = "register";
  private static final String CAT_TOKEN = "token";
  private static final String CAT_ERROR = "error";
  private static final String SENT = "sent";
  private static final String RECEIVED = "received";
  private static final String REGISTERED = "registered";
  private static final String UNREGISTERED = "unregistered";
  private static final String REFRESHED = "refeshed";
  private static final String UPDATED = "updated";
  private static final String NO_SCREEN = "none";

  /**
   * Google Analytics tracker
   */
  private Tracker mTracker;

  /**
   * Get a {@link Tracker}
   * @return tracker
   */
  synchronized public Tracker getTracker() {
    if (mTracker == null) {
      GoogleAnalytics analytics =
        GoogleAnalytics.getInstance(App.getContext());
      // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
      mTracker = analytics.newTracker(TRACKING_ID);
      mTracker.setAppName(AppUtils.getApplicationName());
      mTracker.setAppVersion(Prefs.getVersionName());
    }
    return mTracker;
  }

  /**
   * Generic event
   * @param screen Source screen
   * @param cat    Event category
   * @param action Event action
   * @param label  Event label
   */
  public void event(String screen, String cat, String action, String label) {
    getTracker().setScreenName(screen);
    getTracker().send(new HitBuilders.EventBuilder()
      .setCategory(cat)
      .setAction(action)
      .setLabel(label)
      .build());
  }

  /**
   * Error event
   * @param action Event action
   * @param label  Event label
   */
  public void error(String action, String label) {
    event(NO_SCREEN, CAT_ERROR, action, label);
  }

  /**
   * Exception event
   * @param exception Exception
   * @param message   Error message
   */
  public void exception(Exception exception, String message) {
    String msg = "Caught: ";
    if (!TextUtils.isEmpty(message)) {
      msg += message;
      msg += "\n";
    }
    msg += Log.getStackTraceString(exception);
    getTracker().setScreenName(NO_SCREEN);
    getTracker().send(new HitBuilders.ExceptionBuilder()
      .setFatal(true)
      .setDescription(msg)
      .build());
  }

  /** App updated */
  public void updated() {
    event(NO_SCREEN, CAT_APP, UPDATED, Prefs.getVersionName());
  }

  /**
   * Message sent event
   * @param label message type
   */
  public void sent(String label) {
    event(NO_SCREEN, CAT_MSG, SENT, label);
  }

  /**
   * Message received event
   * @param label message type
   */
  public void received(String label) {
    event(NO_SCREEN, CAT_MSG, RECEIVED, label);
  }

  /** Device registered event */
  public void registered() {
    event(NO_SCREEN, CAT_REG, REGISTERED, "");
  }

  /** Device unregistered event */
  public void unregistered() {
    event(NO_SCREEN, CAT_REG, UNREGISTERED, "");
  }

  /** Firebase token refreshed */
  public void instanceIdRefreshed() {
    event(NO_SCREEN, CAT_TOKEN, REFRESHED, "");
  }
}
