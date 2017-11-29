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
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;

/**
 * BroadcastReceiver that will prevent networks from killing the FCM ports.
 * @see <a href="https://goo.gl/fFSXGy">FCM Keep Alive issue</a>
 */
public class HeartbeatAlarmReceiver extends BroadcastReceiver {
  private final static String TAG = "HeartbeatAlarmReceiver";

  /**
   * Set or cancel the repeating alarm to send the heartbeat
   */
  public static void updateAlarm() {
    final Context ctxt = App.getContext();
    final AlarmManager alarmMgr =
      (AlarmManager) ctxt.getSystemService(Context.ALARM_SERVICE);
    if (alarmMgr == null) {
      Log.logE(ctxt, TAG,
        "Failed to start or cancel " + TAG + ", null AlarmManager");
      return;
    }
    final Intent intent = new Intent(ctxt, HeartbeatAlarmReceiver.class);
    final PendingIntent alarmIntent = PendingIntent.getBroadcast(ctxt,
      Intents.HEARTBEAT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    final long interval = Prefs.INST(ctxt).getHeartbeat() * 60 * 1000;

    if (User.INST(ctxt).isLoggedIn() &&
      Prefs.INST(ctxt).isAllowReceive()) {
      // setup Heartbeat alarm
      alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
        interval, interval, alarmIntent);
      Log.logD(TAG, "Set heartbeat alarm: " + Long.toString(interval));
    } else {
      // cancel
      alarmMgr.cancel(alarmIntent);
      Log.logD(TAG, "Canceled heartbeat alarm");
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    Intent intnt =
      new Intent("com.google.android.intent.action.GTALK_HEARTBEAT");
    context.sendBroadcast(intnt);
    intnt = new Intent("com.google.android.intent.action.MCS_HEARTBEAT");
    context.sendBroadcast(intnt);
    Log.logD(TAG, "onReceive, Sent heartbeat");
  }
}
