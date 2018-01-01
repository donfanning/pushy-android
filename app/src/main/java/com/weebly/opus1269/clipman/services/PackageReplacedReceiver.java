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
import com.weebly.opus1269.clipman.model.Analytics;

/**
 * This {@link BroadcastReceiver} starts the {@link ClipboardWatcherService}
 * and adds a daily alarm when our app is updated
 */
public class PackageReplacedReceiver extends BroadcastReceiver {
  private static final String TAG = "PackageReplacedReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    final String act = intent.getAction();
    if (!"android.intent.action.MY_PACKAGE_REPLACED".equals(act)) {
      return;
    }

    final Context appContext = context.getApplicationContext();

    Log.logD(TAG, "onReceive");

    Analytics.INST(appContext).updated();

    ClipboardWatcherService.startService(appContext, true);

    DailyAlarmReceiver.initialize(TAG, appContext);
  }
}
