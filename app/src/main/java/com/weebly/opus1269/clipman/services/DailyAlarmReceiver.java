/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backup.BackupHelper;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** {@link BroadcastReceiver} that runs once a day */
public class DailyAlarmReceiver extends BroadcastReceiver {
  private static final String TAG = "DailyAlarmReceiver";

  /**
   * Add daily alarm to delete old DB entries and perform backup
   * @param caller  caller's class name
   * @param context a Context
   */
  public static void initialize(String caller, Context context) {
    final AlarmManager alarmMgr =
      (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (alarmMgr == null) {
      Log.logE(context, caller,
        "Failed to start " + TAG + ", null AlarmManager");
      return;
    }
    final Intent intent = new Intent(context, DailyAlarmReceiver.class);
    final PendingIntent alarmIntent =
      PendingIntent.getBroadcast(context, 0, intent, 0);

    // setup daily alarm
    alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
      AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, alarmIntent);


    Log.logD(TAG, "Initialized");

    // run now
    MainRepo.INST(App.INST()).deleteOldClips();
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.logD(TAG, "onReceive");

    if (User.INST(context).isLoggedIn() && Prefs.INST(context).isAutoBackup()) {
      BackupHelper.INST(context).createBackupAsync();
    }

    MainRepo.INST(App.INST()).deleteOldClips();
  }
}
