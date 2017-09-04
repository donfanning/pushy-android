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
  INSTANCE;

  /**
   * Google Analytics tracking ID
   */
  private static final String TRACKING_ID = "UA-61314754-3";

  private static final String CAT_MSG = "message";
  private static final String CAT_REG = "register";
  private static final String CAT_TOKEN = "token";
  private static final String CAT_ERROR = "error";
  private static final String SENT = "sent";
  private static final String RECEIVED = "received";
  private static final String REGISTERED = "registered";
  private static final String UNREGISTERED = "unregistered";
  private static final String REFRESHED = "refeshed";
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
   * Error event
   * @param label  Event label
   * @param action Event action
   */
  public void error(String label, String action) {
    getTracker().setScreenName(NO_SCREEN);
    getTracker().send(new HitBuilders.EventBuilder()
      .setCategory(CAT_ERROR)
      .setAction(action)
      .setLabel(label)
      .build());
  }

  /**
   * Exception
   * @param message Error message
   * @param e       Exception
   */
  public void exception(String message, Exception e) {
    String msg = "Exception caught: ";
    if (!TextUtils.isEmpty(message)) {
      msg = message;
    }
    msg += Log.getStackTraceString(e);
    getTracker().setScreenName(NO_SCREEN);
    getTracker().send(new HitBuilders.ExceptionBuilder()
      .setFatal(true)
      .setDescription(msg)
      .build());
  }

  /**
   * Message sent event
   * @param label message type
   */
  public void sent(String label) {
    getTracker().setScreenName(NO_SCREEN);
    getTracker().send(new HitBuilders.EventBuilder()
      .setCategory(CAT_MSG)
      .setAction(SENT)
      .setLabel(label)
      .build());
  }

  /**
   * Message received event
   * @param label message type
   */
  public void received(String label) {
    getTracker().setScreenName(NO_SCREEN);
    getTracker().send(new HitBuilders.EventBuilder()
      .setCategory(CAT_MSG)
      .setAction(RECEIVED)
      .setLabel(label)
      .build());
  }

  /**
   * Device registered event.
   */
  public void registered() {
    getTracker().setScreenName(NO_SCREEN);
    getTracker().send(new HitBuilders.EventBuilder()
      .setCategory(CAT_REG)
      .setAction(REGISTERED)
      .build());
  }

  /**
   * Device unregistered event.
   */
  public void unregistered() {
    getTracker().setScreenName(NO_SCREEN);
    getTracker().send(new HitBuilders.EventBuilder()
      .setCategory(CAT_REG)
      .setAction(UNREGISTERED)
      .build());
  }

  /**
   * Firebase token refreshed.
   */
  public void instanceIdRefreshed() {
    getTracker().setScreenName(NO_SCREEN);
    getTracker().send(new HitBuilders.EventBuilder()
      .setCategory(CAT_TOKEN)
      .setAction(REFRESHED)
      .build());
  }
}
