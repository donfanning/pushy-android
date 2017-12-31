/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.backup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;

/** Singleton to manage Google DriveHelper data backups */
public class Backup {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static Backup sInstance;

  /** Global Application Context */
  private final Context mContext;

  private final String TAG = this.getClass().getSimpleName();


  private Backup(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static Backup INST(@NonNull Context context) {
    synchronized (Backup.class) {
      if (sInstance == null) {
        sInstance = new Backup(context);
      }
      return sInstance;
    }
  }

}
