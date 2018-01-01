/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Prefs;

/**
 * This {@link BroadcastReceiver} starts the {@link ClipboardWatcherService} at
 * boot time if requested and adds a daily alarm
 * <p>
 * Requires Intent.ACTION_BOOT_COMPLETED
 */
public class AutoStartReceiver extends BroadcastReceiver {
  private static final String TAG = "AutoStartReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
      return;
    }

    final Context appContext = context.getApplicationContext();

    Log.logD(TAG, "onReceive");

    if (Prefs.INST(appContext).isMonitorStartup()) {
      ClipboardWatcherService.startService(appContext, true);
    }

    DailyAlarmReceiver.initialize(TAG, appContext);
  }
}
