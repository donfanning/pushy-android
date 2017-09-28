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

import com.weebly.opus1269.clipman.db.ClipContentProvider;

/**
 * {@link BroadcastReceiver} that cleans up old entries in the database
 */
public class AlarmReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    ClipContentProvider.deleteOldItems();
  }
}
