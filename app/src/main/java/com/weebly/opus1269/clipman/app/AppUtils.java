/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.app;

import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Patterns;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.R;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Random;

/**
 * General static constants utility methods
 */
public class AppUtils {
  private static final String TAG = "AppUtils";

  private static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
  private static final int VERSION_CODE = Build.VERSION.SDK_INT;

  public static final String PLAY_STORE = "market://details?id=" + PACKAGE_NAME;
  public static final String PLAY_STORE_WEB =
    "https://play.google.com/store/apps/details?id=" + PACKAGE_NAME;

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
   * @return boolean
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
   * Launch an {@link Intent} to show a {@link Uri}
   * @param uri A String that is a valid Web Url
   * @return true on success
   */
  public static Boolean showWebUrl(String uri) {
    Boolean ret = false;

    if (Patterns.WEB_URL.matcher(uri).matches()) {
      ret = true;
      final Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setData(Uri.parse(uri));
      try {
        App.getContext().startActivity(intent);
      } catch (Exception ex) {
        Log.logEx(TAG, "", ex);
        ret = false;
      }
    }
    return ret;
  }

  /**
   * Launch an {@link Intent} to search the web
   * @param text A String to search for
   * @return true on success
   */
  public static Boolean performWebSearch(String text) {
    boolean ret = true;
    final Context context = App.getContext();

    if (TextUtils.isEmpty(text)) {
      ret = false;
    } else {
      final Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
      intent.putExtra(SearchManager.QUERY, text);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      try {
        context.startActivity(intent);
      } catch (Exception ex) {
        Log.logEx(TAG, "", ex);
        ret = false;
      }
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
