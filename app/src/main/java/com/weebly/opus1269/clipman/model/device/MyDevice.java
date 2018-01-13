/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Prefs;

import org.threeten.bp.Instant;

/** Singleton representing our device */
public class MyDevice implements Device {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static MyDevice sInstance;

  /** Global Application Context */
  private final Context mContext;

  private MyDevice(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static MyDevice INST(@NonNull Context context) {
    synchronized (MyDevice.class) {
      if (sInstance == null) {
        sInstance = new MyDevice(context);
      }
      return sInstance;
    }
  }

  public String getModel() {
    String value;
    final String manufacturer = Build.MANUFACTURER;
    final String model = Build.MODEL;
    if (model.startsWith(manufacturer)) {
      value = AppUtils.capitalize(model);
    } else {
      value = AppUtils.capitalize(manufacturer) + " " + model;
    }
    if (value.startsWith("Htc ")) {
      // special case for HTC
      value = value.replaceFirst("Htc ", "HTC ");
    }
    return value;
  }

  public String getSN() {
    return Prefs.INST(mContext).getSN();
  }

  public String getOS() {
    return "Android";
  }

  public String getNickname() {
    return Prefs.INST(mContext).getDeviceNickname();
  }

  public long getLastSeen() {
    return Instant.now().getEpochSecond();
  }

  /** A String suitable for display */
  public String getDisplayName() {
    String name = getNickname();
    if (TextUtils.isEmpty(name)) {
      name = getModel() + " - " + getSN() + " - " + getOS();
    }
    return name;
  }

  /** A String that is unique for a Device */
  public String getUniqueName() {
    return getModel() + " - " + getSN() + " - " + getOS();
  }
}
