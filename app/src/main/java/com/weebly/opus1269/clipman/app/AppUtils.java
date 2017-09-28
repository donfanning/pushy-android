/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.R;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Random;

/**
 * General static constants and utility methods
 */
public class AppUtils {
  private static final String TAG = "AppUtils";

  private static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
  private static final String PLAY_STORE =
    "market://details?id=" + PACKAGE_NAME;
  private static final String PLAY_STORE_WEB =
    "https://play.google.com/store/apps/details?id=" + PACKAGE_NAME;
  private static final int VERSION_CODE = Build.VERSION.SDK_INT;
  private static final String ERROR_ACTIVITY = "Failed to start activity";

  private AppUtils() {
  }

  /**
   * Get the app name
   * @return app name
   */
  public static String getApplicationName() {
    final Context context = App.getContext();
    final int stringId = context.getApplicationInfo().labelRes;
    return context.getString(stringId);
  }

  /**
   * Check if we are running on an oreo or newer device
   * @return boolean
   */
  public static boolean isOreoOrLater() {
    return VERSION_CODE >= Build.VERSION_CODES.O;
  }

  /**
   * Check if we are running on a lollipop or newer device
   * @return boolean
   */
  public static boolean isLollipopOrLater() {
    return VERSION_CODE >= Build.VERSION_CODES.LOLLIPOP;
  }

  /**
   * Check if we are running on a jellybean or newer device
   * @return boolean
   */
  public static boolean isJellyBeanOrLater() {
    return VERSION_CODE >= Build.VERSION_CODES.JELLY_BEAN;
  }

  /**
   * Check if the MainActivity is in dual pane mode
   * @return boolean
   */
  public static boolean isDualPane() {
    return App.getContext().getResources().getBoolean(R.bool.dual_pane);
  }

  /**
   * Check if a service is running
   * @param serviceClass Class name of Service
   * @return true if service is running
   * @see <a href="https://goo.gl/55RFa6">Stack Overflow</a>
   */
  public static boolean isMyServiceRunning(Class<?> serviceClass) {
    final Context context = App.getContext();
    final ActivityManager manager = (ActivityManager)
      context.getSystemService(Context.ACTIVITY_SERVICE);
    boolean ret = false;

    for (final ActivityManager.RunningServiceInfo service :
      manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        ret = true;
        break;
      }
    }

    return ret;
  }

  /**
   * Try to start an activity from another activity
   * @param activity starting activity
   * @param intent   Activity intent
   * @param notify   notify user on error if true
   * @return true if successful
   */
  public static boolean startActivity(Activity activity, Intent intent,
                                      boolean notify) {
    boolean ret = true;

    try {
      activity.startActivity(intent);
    } catch (Exception ex) {
      final String msg = activity.getString(R.string.err_start_activity);
      Log.logEx(TAG, msg, ex, ERROR_ACTIVITY, notify);
      ret = false;
    }

    return ret;
  }

  /**
   * Try to start an activity from another activity
   * @param activity starting activity
   * @param intent   Activity intent
   * @return true if successful
   */
  public static boolean startActivity(Activity activity, Intent intent) {
    return startActivity(activity, intent, true);
  }

  /**
   * Try to start an activity as a new task
   * @param intent Activity intent
   * @param notify if true, notify user on error
   * @return true if successful
   */
  public static boolean startNewTaskActivity(Intent intent, boolean notify) {
    Context context = App.getContext();
    boolean ret = true;

    try {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
    } catch (Exception ex) {
      final String msg = context.getString(R.string.err_start_activity);
      Log.logEx(TAG, msg, ex, ERROR_ACTIVITY, notify);
      ret = false;
    }

    return ret;
  }

  /**
   * Try to start an activity as a new task
   * @param intent Activity intent
   * @return true if successful
   */
  public static boolean startNewTaskActivity(Intent intent) {
    return startNewTaskActivity(intent, true);
  }

  /**
   * Display a toast or snackbar message
   * @param view a view to use for snackbar
   * @param msg  message to display
   */
  public static void showMessage(final View view, final String msg) {
    final Context context = App.getContext();
    if (view != null) {
      Snackbar
        .make(view, msg, Snackbar.LENGTH_SHORT)
        .show();
    } else {
      Handler handler = new Handler(context.getMainLooper());
      handler.post(new Runnable() {
        public void run() {
          Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
      });
    }
  }

  /**
   * Show the {@link App} in the play store
   */
  public static void showInPlayStore() {
    final Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(PLAY_STORE));
    if (!AppUtils.startNewTaskActivity(intent, false)) {
      Log.logD(TAG, "Could not open app in play store, trying web.");
      AppUtils.showWebUrl(PLAY_STORE_WEB);
    }
  }

  /**
   * Launch an {@link Intent} to show a {@link Uri}
   * @param uri A String that is a valid Web Url
   * @return true on success
   */
  public static Boolean showWebUrl(String uri) {
    Boolean ret = false;

    if (Patterns.WEB_URL.matcher(uri).matches()) {
      final Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse(uri));
      ret = AppUtils.startNewTaskActivity(intent);
    }
    return ret;
  }

  /**
   * Launch an {@link Intent} to search the web
   * @param text A String to search for
   * @return true on success
   */
  public static Boolean performWebSearch(String text) {
    boolean ret;

    if (TextUtils.isEmpty(text)) {
      ret = false;
    } else {
      final Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
      intent.putExtra(SearchManager.QUERY, text);
      ret = AppUtils.startNewTaskActivity(intent);
    }
    return ret;
  }

  /**
   * Get a time string relative to now
   * @param date A {@link DateTime}
   * @return CharSequence time
   */
  public static CharSequence getRelativeDisplayTime(DateTime date) {
    final Context context = App.getContext();
    final CharSequence value;
    long now = System.currentTimeMillis();
    long time = date.getMillis();
    long delta = now - time;

    if (delta <= DateUtils.SECOND_IN_MILLIS) {
      DateTimeFormatter fmt =
        DateTimeFormat.forPattern(context.getString(R.string.joda_time_fmt_pattern));
      value = context.getString(R.string.now_fmt, date.toString(fmt));
    } else {
      value =
        DateUtils.getRelativeDateTimeString(context, time,
          DateUtils.SECOND_IN_MILLIS, DateUtils.DAY_IN_MILLIS,
          DateUtils.FORMAT_ABBREV_ALL);
    }
    return value;
  }

  /**
   * Is String null or all whitespace
   * @param string string to check
   */
  public static boolean isWhitespace(String string) {
    boolean ret = true;
    if (string != null) {
      for (int i = 0; i < string.length(); i++) {
        if (!Character.isWhitespace(string.charAt(i))) {
          ret = false;
        }
      }
    }
    return ret;
  }

  /**
   * Capitalize a {@link String}
   * @param s String to captialize
   * @return capitalized String
   */
  public static String capitalize(String s) {
    if (TextUtils.isEmpty(s)) {
      return "";
    }
    final char first = s.charAt(0);
    if (Character.isUpperCase(first)) {
      return s;
    } else {
      return Character.toUpperCase(first) + s.substring(1);
    }
  }

  /**
   * Get a pseudo-random string of the given length
   * @param length length of string to generate
   * @return a pseudo-random string
   */
  public static String getRandomString(int length) {
    char[] chars1 = "ABCDEF012GHIJKL345MNOPQR678STUVWXYZ9".toCharArray();
    StringBuilder sb1 = new StringBuilder();
    Random random1 = new Random();
    for (int i = 0; i < length; i++) {
      char c1 = chars1[random1.nextInt(chars1.length)];
      sb1.append(c1);
    }
    return sb1.toString();
  }

  /**
   * Get a pseudo-random string of a fixed length
   * @return a pseudo-random string
   */
  public static String getRandomString() {
    return getRandomString(8);
  }

  /**
   * Convert device density to pixels
   * @param context  A Context
   * @param dipValue Value to convert
   * @return Value in pixels
   */
  public static int dp2px(Context context, float dipValue) {
    final float scale = context.getResources().getDisplayMetrics().density;
    //noinspection NumericCastThatLosesPrecision,MagicNumber
    return (int) ((dipValue * scale) + 0.5F);
  }

  /**
   * Convert pixels to device density
   * @param context A Context
   * @param pxValue Value to convert
   * @return Value in device density
   */
  @SuppressWarnings("unused")
  public static int px2dp(Context context, float pxValue) {
    final float scale = context.getResources().getDisplayMetrics().density;
    //noinspection NumericCastThatLosesPrecision,MagicNumber
    return (int) ((pxValue / scale) + 0.5F);
  }
}
