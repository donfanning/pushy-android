/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.db.ClipsDatabaseHelper;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.services.HeartbeatAlarmReceiver;
import com.weebly.opus1269.clipman.ui.devices.DevicesActivity;
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extend the App class so we can get a {@link Context} anywhere
 * and perform some initialization
 */
public class App extends Application implements
  Application.ActivityLifecycleCallbacks,
  SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = "App";

  // OK, because App won't go away until killed
  @SuppressLint("StaticFieldLeak")
  private static Context sContext = null;
  @SuppressLint("StaticFieldLeak")
  private static ClipsDatabaseHelper sClipsDB = null;

  private static boolean sIsMainActivityVisible = false;
  private static boolean sIsDevicesActivityVisible = false;

  /**
   * Maps between an activity class name and the list of currently running
   * AsyncTasks that were spawned while it was active.
   */
  private final Map<String, List<CustomAsyncTask<?, ?, ?>>> mActivityTaskMap;

  public App() {
    mActivityTaskMap = new HashMap<>();
  }

  public static Context getContext() {
    return sContext;
  }

  public static ClipsDatabaseHelper getDbHelper() {
    return sClipsDB;
  }

  public static boolean isMainActivityVisible() {
    return sIsMainActivityVisible;
  }

  public static boolean isDevicesActivityVisible() {
    return sIsDevicesActivityVisible;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    sContext = this;

    // initialize database
    sClipsDB = new ClipsDatabaseHelper(sContext);
    sClipsDB.getWritableDatabase();

    // make sure Prefs are initialized
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    // reset fav filter
    Prefs.INST(sContext).setFavFilter(false);

    // reset label filter
    Prefs.INST(sContext).setLabelFilter("");

    // save version info. to the preferences database
    final PackageInfo pInfo;
    try {
      pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

      // update preferences on version change
      updatePreferences(pInfo.versionCode);

      // save current version
      Prefs.INST(sContext).setVersionName(pInfo.versionName);
      Prefs.INST(sContext).setVersionCode(pInfo.versionCode);
    } catch (final PackageManager.NameNotFoundException ex) {
      Log.logEx(TAG, "Version info not found: " + ex.getMessage(), ex, false);
    }

    // Register to be notified of activity state changes
    registerActivityLifecycleCallbacks(this);

    // Initialize the Notification Channels
    Notifications.initChannels(this);

    // Setup heartbeat alarm
    HeartbeatAlarmReceiver.updateAlarm();

    // listen for preference changes
    PreferenceManager
      .getDefaultSharedPreferences(getContext())
      .registerOnSharedPreferenceChangeListener(this);
  }

  /** unused */
  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
  }

  /** unused */
  @Override
  public void onActivityStarted(Activity activity) {
  }

  @Override
  public void onActivityResumed(Activity activity) {
    if (activity instanceof MainActivity) {
      sIsMainActivityVisible = true;
    } else if (activity instanceof DevicesActivity) {
      sIsDevicesActivityVisible = true;
    }
  }

  @Override
  public void onActivityPaused(Activity activity) {
    if (activity instanceof MainActivity) {
      sIsMainActivityVisible = false;
    } else if (activity instanceof DevicesActivity) {
      sIsDevicesActivityVisible = false;
    }
  }

  /** unused */
  @Override
  public void onActivityStopped(Activity activity) {
  }

  /** unused */
  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
  }

  /** unused */
  @Override
  public void onActivityDestroyed(Activity activity) {
  }

  @Override
  public void
  onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    final String keyUserId = User.INST(sContext).PREF_USER_ID;
    final String keyReceive = getString(R.string.key_pref_receive_msg);
    final String keyHeartbeat = getString(R.string.key_pref_heartbeat);

    // update heartbeat on appropriate changes
    if (key.equals(keyUserId)) {
      HeartbeatAlarmReceiver.updateAlarm();
    } else if (key.equals(keyReceive)) {
      HeartbeatAlarmReceiver.updateAlarm();
    } else if (key.equals(keyHeartbeat)) {
      HeartbeatAlarmReceiver.updateAlarm();
    }
  }

  /**
   * These methods are part of a solution to the problem of screen
   * orientation/Activity destruction during lengthy Async tasks.
   * https://goo.gl/vsNa1h
   */

  public void removeTask(CustomAsyncTask<?, ?, ?> task) {
    for (Map.Entry<String, List<CustomAsyncTask<?, ?, ?>>>
      entry : mActivityTaskMap.entrySet()) {
      List<CustomAsyncTask<?, ?, ?>> tasks = entry.getValue();
      for (int i = 0; i < tasks.size(); i++) {
        if (tasks.get(i) == task) {
          tasks.remove(i);
          break;
        }
      }

      if (tasks.size() == 0) {
        mActivityTaskMap.remove(entry.getKey());
        return;
      }
    }
  }

  public void addTask(Activity activity, CustomAsyncTask<?, ?, ?> task) {
    String key = activity.getClass().getCanonicalName();
    List<CustomAsyncTask<?, ?, ?>> tasks = mActivityTaskMap.get(key);
    if (tasks == null) {
      tasks = new ArrayList<>();
      mActivityTaskMap.put(key, tasks);
    }

    tasks.add(task);
  }

  public void detach(Activity activity) {
    List<CustomAsyncTask<?, ?, ?>> tasks =
      mActivityTaskMap.get(activity.getClass().getCanonicalName());
    if (tasks != null) {
      for (CustomAsyncTask<?, ?, ?> task : tasks) {
        task.setActivity(null);
      }
    }
  }

  public void attach(Activity activity) {
    List<CustomAsyncTask<?, ?, ?>> tasks =
      mActivityTaskMap.get(activity.getClass().getCanonicalName());
    if (tasks != null) {
      for (CustomAsyncTask<?, ?, ?> task : tasks) {
        task.setActivity(activity);
      }
    }
  }

  /**
   * Update preferences on version change. If we add or change preferences
   * this is where we can make sure they are initialized properly
   * @param versionCode the current version code
   */
  private void updatePreferences(final int versionCode) {
    Log.logD(TAG, "updatePreferences called");
    final int oldVersionCode = Prefs.INST(sContext).getVersionCode();

    if ((oldVersionCode == 0) || (versionCode == oldVersionCode)) {
      Log.logD(TAG, "no change needed");
      return;
    }

    if (oldVersionCode <= 15005) {
      // Enable Error notifications
      final SharedPreferences preferences =
        PreferenceManager.getDefaultSharedPreferences(this);
      final String key = getString(R.string.key_pref_not_types);
      final String errorValue = getString(R.string.ar_not_error_value);
      final Set<String> values =
        preferences.getStringSet(key, Prefs.INST(sContext).DEF_NOTIFICATIONS);
      if (!values.contains(errorValue)) {
        // add the error notification
        values.add(errorValue);
        preferences.edit()
          .putStringSet(key, values)
          .apply();
        Log.logD(TAG, "enabled On Error notification value");
      }
    }
  }
}
