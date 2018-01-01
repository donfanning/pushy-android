/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Singleton to manage preferencses */
public class Prefs {

  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static Prefs sInstance;

  /** Global Application Context */
  private final Context mContext;

  private final String DEF_THEME;
  private final String DEF_DURATION;
  private final String DEF_RINGTONE;

  // app notification types
  private final String NOTIFY_REMOTE_COPY;
  private final String NOTIFY_LOCAL_COPY;
  private final String NOTIFY_DEVICE_ADDED;
  private final String NOTIFY_DEVICE_REMOVED;
  private final String NOTIFY_ERROR;

  public final Set<String> DEF_NOTIFICATIONS;

  // Preferences that are not set through the SettingsActivity
  private final String PREF_VERSION_NAME = "prefVersionName";
  private final String PREF_VERSION_CODE = "prefVersionCode";
  private final String PREF_PIN_FAV = "prefPinFav";
  private final String PREF_FAV_FILTER = "prefFavFilter";
  private final String PREF_LABEL_FILTER = "preflabelFilter";
  private final String PREF_SORT_TYPE = "prefSortType";
  private final String PREF_DEVICE_REGISTERED = "prefDeviceRegistered";
  private final String PREF_DEVICES = "prefDevices";
  private final String PREF_NO_DEVICES_CT = "prefNoDeviceCt";
  private final String PREF_SN = "prefSN";
  private final String PREF_LAST_BACKUP = "prefLastBackup";


  private Prefs(@NonNull Context context) {
    mContext = context.getApplicationContext();

    DEF_THEME = mContext.getString(R.string.ar_theme_light_value);
    DEF_DURATION = mContext.getString(R.string.ar_duration_forever_value);
    DEF_RINGTONE = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
    NOTIFY_REMOTE_COPY = mContext.getString(R.string.ar_not_remote_value);
    NOTIFY_LOCAL_COPY = mContext.getString(R.string.ar_not_local_value);
    NOTIFY_DEVICE_ADDED = mContext.getString(R.string.ar_not_dev_added_value);
    NOTIFY_DEVICE_REMOVED =
      mContext.getString(R.string.ar_not_dev_removed_value);
    NOTIFY_ERROR = mContext.getString(R.string.ar_not_error_value);
    final String[] DEF_NOTIFY_VALUES = mContext.getResources()
      .getStringArray(R.array.pref_not_types_default_values);
    DEF_NOTIFICATIONS = new HashSet<>(Arrays.asList(DEF_NOTIFY_VALUES));
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static Prefs INST(@NonNull Context context) {
    synchronized (Prefs.class) {
      if (sInstance == null) {
        sInstance = new Prefs(context);
      }
      return sInstance;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Set from UI
  ///////////////////////////////////////////////////////////////////////////

  public boolean isMonitorStartup() {
    final String key = mContext.getString(R.string.key_pref_monitor_startup);
    return get(key, true);
  }

  public boolean isMonitorClipboard() {
    final String key = mContext.getString(R.string.key_pref_monitor_clipboard);
    return get(key, true);
  }

  public boolean isPushClipboard() {
    final String key = mContext.getString(R.string.key_pref_push_msg);
    return get(key, true);
  }

  @SuppressWarnings("SameParameterValue")
  public void setPushClipboard(boolean value) {
    final String key = mContext.getString(R.string.key_pref_push_msg);
    set(key, value);
  }

  public boolean isAutoSend() {
    final String key = mContext.getString(R.string.key_pref_auto_msg);
    return get(key, true);
  }

  public boolean isAllowReceive() {
    final String key = mContext.getString(R.string.key_pref_receive_msg);
    return get(key, true);
  }

  public int getHeartbeat() {
    final String key = mContext.getString(R.string.key_pref_heartbeat);
    return Integer.parseInt(get(key, "5"));
  }

  public boolean isHighPriority() {
    final String key = mContext.getString(R.string.key_pref_priority_msg);
    return get(key, true);
  }

  public String getDeviceNickname() {
    final String key = mContext.getString(R.string.key_pref_nickname);
    return get(key, "");
  }

  public String getDuration() {
    final String key = mContext.getString(R.string.key_pref_duration);
    return get(key, DEF_DURATION);
  }

  public boolean isAutoBackup() {
    final String key = mContext.getString(R.string.key_pref_auto_backup);
    return get(key, false);
  }

  public void unsetAutoBackup() {
    final String key = mContext.getString(R.string.key_pref_auto_backup);
    set(key, false);
  }


  private String getTheme() {
    final String key = mContext.getString(R.string.key_pref_theme);
    return get(key, DEF_THEME);
  }

  public boolean isDarkTheme() {
    return mContext.getString(R.string.ar_theme_dark_value).equals(getTheme());
  }

  public boolean isLightTheme() {
    return mContext.getString(R.string.ar_theme_light_value).equals(getTheme());
  }

  public boolean notNotifications() {
    final String key = mContext.getString(R.string.key_pref_notifications);
    return !get(key, true);
  }

  boolean isNotifyLocal() {
    return isNotifyEnabled(NOTIFY_LOCAL_COPY);
  }

  boolean isNotifyRemote() {
    return isNotifyEnabled(NOTIFY_REMOTE_COPY);
  }

  boolean isNotifyDeviceAdded() {
    return isNotifyEnabled(NOTIFY_DEVICE_ADDED);
  }

  boolean isNotifyDeviceRemoved() {
    return isNotifyEnabled(NOTIFY_DEVICE_REMOVED);
  }

  public boolean isNotifyError() {
    return isNotifyEnabled(NOTIFY_ERROR);
  }

  private boolean isNotifyEnabled(String value) {
    if (notNotifications()) {
      return false;
    }
    final String key = mContext.getString(R.string.key_pref_not_types);
    final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
    final Set<String> values = preferences.getStringSet(key, DEF_NOTIFICATIONS);
    return values.contains(value);
  }

  boolean isAudibleOnce() {
    final String key = mContext.getString(R.string.key_pref_not_audible_once);
    return get(key, true);
  }

  @Nullable
  Uri getNotificationSound() {
    Uri ret = null;
    final String value = getRingtone();
    if (!TextUtils.isEmpty(value)) {
      ret = Uri.parse(value);
    }
    return ret;
  }

  public String getRingtone() {
    final String key =
      mContext.getString(R.string.key_pref_ringtone);
    return get(key, DEF_RINGTONE);
  }

  public void setRingtone(String value) {
    final String key =
      mContext.getString(R.string.key_pref_ringtone);
    set(key, value);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Not set from Settings UI
  ///////////////////////////////////////////////////////////////////////////

  public String getVersionName() {
    return get(PREF_VERSION_NAME, "");
  }

  public void setVersionName(String value) {
    set(PREF_VERSION_NAME, value);
  }

  public int getVersionCode() {
    return get(PREF_VERSION_CODE, 0);
  }

  public void setVersionCode(int value) {
    set(PREF_VERSION_CODE, value);
  }

  public int getSortType() {
    return get(PREF_SORT_TYPE, 0);
  }

  public void setSortType(int value) {
    set(PREF_SORT_TYPE, value);
  }

  public boolean isPinFav() {
    return get(PREF_PIN_FAV, false);
  }

  public void setPinFav(Boolean value) {
    set(PREF_PIN_FAV, value);
  }

  public boolean isFavFilter() {
    return get(PREF_FAV_FILTER, false);
  }

  public void setFavFilter(Boolean value) {
    set(PREF_FAV_FILTER, value);
  }

  public String getLabelFilter() {
    return get(PREF_LABEL_FILTER, "");
  }

  public void setLabelFilter(String value) {
    set(PREF_LABEL_FILTER, value);
  }

  public boolean isDeviceRegistered() {
    return get(PREF_DEVICE_REGISTERED, false);
  }

  public void setDeviceRegistered(Boolean value) {
    set(PREF_DEVICE_REGISTERED, value);
  }

  public String getDevices() {
    return get(PREF_DEVICES, "");
  }

  public void setDevices(String value) {
    set(PREF_DEVICES, value);
  }

  public int getNoDevicesCt() {
    return get(PREF_NO_DEVICES_CT, 0);
  }

  public void setNoDevicesCt(int value) {
    set(PREF_NO_DEVICES_CT, value);
  }

  /** Get Serial number. Create if not set */
  String getSN() {
    final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
    String sN;

    if (preferences.contains(PREF_SN)) {
      // already set
      sN = preferences.getString(PREF_SN, "");
    } else {
      setSN();
      sN = preferences.getString(PREF_SN, "");
    }
    return sN;
  }

  /** Set Serial number to unique value */
  void setSN() {
    String sN;

    // generate unique serial number for life of install at least
    // TODO may need to replace at some point
    sN = Build.SERIAL;
    if (sN.isEmpty() || (sN.equals("unknown"))) {
      // unknown is what emulators return
      sN = AppUtils.getRandomString();
    }
    // set now. will never change unless re-installed
    set(PREF_SN, sN);
  }

  public String getLastBackup() {
    return get(PREF_LAST_BACKUP, "");
  }

  public void setLastBackup(String value) {
    set(PREF_LAST_BACKUP, value);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Setter and getter helpers
  ///////////////////////////////////////////////////////////////////////////

  void set(String key, String value) {
    final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
    preferences.edit().putString(key, value).apply();
  }

  private void set(String key, boolean value) {
    final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
    preferences.edit().putBoolean(key, value).apply();
  }

  private void set(String key, int value) {
    final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
    preferences.edit().putInt(key, value).apply();
  }

  String get(String key, String defValue) {
    final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
    return preferences.getString(key, defValue);
  }

  private boolean get(String key, boolean defValue) {
    final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
    return preferences.getBoolean(key, defValue);
  }

  @SuppressWarnings("SameParameterValue")
  private int get(String key, int defValue) {
    final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
    return preferences.getInt(key, defValue);
  }

  void remove(String key) {
    final SharedPreferences preferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
    preferences.edit().remove(key).apply();
  }
}

