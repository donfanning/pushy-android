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

import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Prefs;

public class PackageReplacedReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    final String act = intent.getAction();
    if (!"android.intent.action.MY_PACKAGE_REPLACED".equals(act)) {
      return;
    }

    Analytics.INSTANCE.updated();

    setupClipboardWatcher();
  }

  /**
   * Start service to monitor Clipboard for changes
   */
  private static void setupClipboardWatcher() {
    if (Prefs.isMonitorClipboard()) {
      ClipboardWatcherService.startService(true);
    }
  }
}
