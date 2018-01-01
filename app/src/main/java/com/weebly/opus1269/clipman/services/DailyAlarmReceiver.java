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

import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.ClipTable;

/** {@link BroadcastReceiver} that cleans up old entries in the database */
public class DailyAlarmReceiver extends BroadcastReceiver {
  private static final String TAG = "DailyAlarmReceiver";

  /**
   * Add daily alarm to cleanup database of old entries
   * @param caller  caller's class name
   * @param ctxt a Context
   */
  public static void initialize(String caller, Context ctxt) {
    final AlarmManager alarmMgr =
      (AlarmManager) ctxt.getSystemService(Context.ALARM_SERVICE);
    if (alarmMgr == null) {
      Log.logE(ctxt, caller,
        "Failed to start " + TAG + ", null AlarmManager");
      return;
    }
    final Intent intent = new Intent(ctxt, DailyAlarmReceiver.class);
    final PendingIntent alarmIntent =
      PendingIntent.getBroadcast(ctxt, 0, intent, 0);

    // setup daily alarm
    alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
      AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, alarmIntent);

    // run now
    ClipTable.INST(ctxt).deleteOldItems();

    Log.logD(TAG, "Initialized");
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.logD(TAG, "onReceive");

    ClipTable.INST(context).deleteOldItems();
  }
}
