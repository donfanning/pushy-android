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

/** Singleton to manage interactions with Google Drive */
public class Drive {
  // OK, because mContext is the global Application context
  /** Static instance */
  @SuppressLint("StaticFieldLeak")
  private static Drive sInstance;

  /** Global Application Context */
  private final Context mContext;

  /** Class Indentifier */
  private final String TAG = this.getClass().getSimpleName();


  private Drive(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static Drive INST(@NonNull Context context) {
    synchronized (Drive.class) {
      if (sInstance == null) {
        sInstance = new Drive(context);
      }
      return sInstance;
    }
  }
}
