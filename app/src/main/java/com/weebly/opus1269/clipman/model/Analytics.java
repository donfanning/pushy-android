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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

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
  public static final String UI_EDIT_TEXT = "textChanged";
  private static final String UI_CLICK = "click";
  private static final String UI_BUTTON = "buttonClicked";
  private static final String UI_CHECKBOX = "checkBoxClicked";
  private static final String UI_IMAGE_VIEW = "imageButtonClicked";
  private static final String UI_MENU = "menuSelect";

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
      final Context context = App.getContext();
      GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
      // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
      mTracker = analytics.newTracker(TRACKING_ID);
      mTracker.setAppName(AppUtils.getApplicationName());
      mTracker.setAppVersion(Prefs.INST(context).getVersionName());
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
    final HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder();

    builder
      .setCategory(cat)
      .setAction(action);

    if (!TextUtils.isEmpty(label)) {
      builder.setLabel(label);
    }

    getTracker().setScreenName(screen);
    getTracker().send(builder.build());
  }

  /**
   * Generic event
   * @param screen Source screen
   * @param cat    Event category
   * @param action Event action
   */
  public void event(String screen, String cat, String action) {
    event(screen, cat, action, null);
  }

  /**
   * Generic click even
   * @param screen Source screen
   * @param label  description
   */
  public void click(String screen, String label) {
    event(screen, Analytics.CAT_UI, Analytics.UI_CLICK, label);
  }

  /**
   * Button clicked event
   * @param screen Source screen
   * @param view   Source screen
   */
  public void buttonClick(String screen, View view) {
    if (view instanceof Button) {
      final String label = ((Button) view).getText().toString();
      event(screen, Analytics.CAT_UI, Analytics.UI_BUTTON, label);
    }
  }

  /**
   * Button clicked event
   * @param screen Source screen
   * @param item   Source screen
   */
  public void menuClick(String screen, MenuItem item) {
    String label = "";
    if (item.getItemId() == Menu.NONE) {
      label = "label";
    } else {
      final CharSequence title = item.getTitle();
      if (title != null) {
        label = title.toString();
      }
    }
    event(screen, Analytics.CAT_UI, Analytics.UI_MENU, label);
  }

  /**
   * ImageView clicked event
   * @param screen Source screen
   * @param label  description
   */
  public void imageClick(String screen, String label) {
    event(screen, Analytics.CAT_UI, Analytics.UI_IMAGE_VIEW, label);
  }

  /**
   * CheckboxView clicked event
   * @param screen Source screen
   * @param label  description
   */
  public void checkBoxClick(String screen, String label) {
    event(screen, Analytics.CAT_UI, Analytics.UI_CHECKBOX, label);
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

  /**
   * App updated
   * @param context
   */
  public void updated(Context context) {
    event(NO_SCREEN, CAT_APP, UPDATED, Prefs.INST(context).getVersionName());
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
    event(NO_SCREEN, CAT_REG, REGISTERED);
  }

  /** Device unregistered event */
  public void unregistered() {
    event(NO_SCREEN, CAT_REG, UNREGISTERED);
  }

  /** Firebase token refreshed */
  public void instanceIdRefreshed() {
    event(NO_SCREEN, CAT_TOKEN, REFRESHED);
  }
}
