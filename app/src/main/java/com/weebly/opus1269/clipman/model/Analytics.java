/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.weebly.opus1269.clipman.app.AppUtils;

/**
 * Singleton for Google Analytics tracking.
 * @see <a href="https://goo.gl/VUowF7">Android Analytics</a>
 */
public class Analytics {

  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static Analytics sInstance;

  public final String CAT_UI = "ui";
  public final String UI_TOGGLE = "toggle";
  public final String UI_LIST = "listSelect";
  public final String UI_MULTI_LIST = "multiListSelect";
  public final String UI_EDIT_TEXT = "textChanged";
  public final String DB_LABEL = "label";
  public final String DB_CLIP_ITEM = "clipItem";
  public final String DB_CREATE = "create";
  public final String DB_CREATE_OR_UPDATE = "createOrUpdate";
  public final String DB_UPDATE = "update";
  public final String DB_DELETE = "delete";
  /** Global Application Context */
  private final Context mContext;

  private final String CAT_MSG = "message";
  private final String CAT_REG = "register";

  private final String NO_SCREEN = "none";

  /** Google Analytics tracker */
  private Tracker mTracker;

  private Analytics(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static Analytics INST(@NonNull Context context) {
    synchronized (Analytics.class) {
      if (sInstance == null) {
        sInstance = new Analytics(context);
      }
      return sInstance;
    }
  }

  /**
   * Get a {@link Tracker}
   * @return tracker
   */
  synchronized private Tracker getTracker() {
    if (mTracker == null) {
      final String TRACKING_ID = "UA-61314754-3";
      GoogleAnalytics analytics = GoogleAnalytics.getInstance(mContext);
      // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
      mTracker = analytics.newTracker(TRACKING_ID);
      mTracker.setAppName(AppUtils.getAppName(mContext));
      mTracker.setAppVersion(Prefs.INST(mContext).getVersionName());
    }
    return mTracker;
  }

  /**
   * Screen view
   * @param screen Source screen
   */
  public void screen(String screen) {
    getTracker().setScreenName(screen);
    getTracker().send(new HitBuilders.ScreenViewBuilder().build());
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
   * Generic  event
   * @param screen Source screen
   * @param cat    Event category
   * @param action Event action
   */
  private void event(String screen, String cat, String action) {
    event(screen, cat, action, null);
  }

  /**
   * Database event
   * @param action Event action
   * @param label  Event label
   */
  public void eventDB(String action, String label) {
    event(NO_SCREEN, "database", action, label);
  }

  /**
   * Generic click even
   * @param screen Source screen
   * @param label  description
   */
  public void click(String screen, String label) {
    event(screen, CAT_UI, "click", label);
  }

  /**
   * Button clicked event
   * @param screen Source screen
   * @param view   Source screen
   */
  public void buttonClick(String screen, View view) {
    if (view instanceof Button) {
      final String label = ((Button) view).getText().toString();
      event(screen, CAT_UI, "buttonClicked", label);
    }
  }

  /**
   * Button clicked event
   * @param screen Source screen
   * @param text   button text
   */
  public void buttonClick(String screen, String text) {
    event(screen, CAT_UI, "buttonClicked", text);
  }

  /**
   * Menu clicked event
   * @param screen Source screen
   * @param item   Source screen
   */
  public void menuClick(String screen, MenuItem item) {
    String label = screen + '.';
    if (item.getItemId() == Menu.NONE) {
      label += "label";
    } else {
      final CharSequence title = item.getTitle();
      if (title != null) {
        label += title.toString();
      }
    }
    event(screen, CAT_UI, "menuSelect", label);
  }

  /**
   * ImageView clicked event
   * @param screen Source screen
   * @param label  description
   */
  public void imageClick(String screen, String label) {
    event(screen, CAT_UI, "imageButtonClicked", label);
  }

  /**
   * CheckboxView clicked event
   * @param screen Source screen
   * @param label  description
   */
  public void checkBoxClick(String screen, String label) {
    event(screen, CAT_UI, "checkBoxClicked", label);
  }

  /**
   * Error event
   * @param action Event action
   * @param label  Event label
   */
  public void error(String action, String label) {
    event(NO_SCREEN, "error", action, label);
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
   */
  public void updated() {
    event(NO_SCREEN, "app", "updated",
      Prefs.INST(mContext).getVersionName());
  }

  /**
   * Message sent event
   * @param label message type
   */
  public void sent(String label) {
    event(NO_SCREEN, CAT_MSG, "sent", label);
  }

  /**
   * Message received event
   * @param label message type
   */
  public void received(String label) {
    event(NO_SCREEN, CAT_MSG, "received", label);
  }

  /** Device registered event */
  public void registered() {
    event(NO_SCREEN, CAT_REG, "registered");
  }

  /** Device unregistered event */
  public void unregistered() {
    event(NO_SCREEN, CAT_REG, "unregistered");
  }

  /** Firebase token refreshed */
  public void instanceIdRefreshed() {
    event(NO_SCREEN, "token", "refreshed");
  }
}
