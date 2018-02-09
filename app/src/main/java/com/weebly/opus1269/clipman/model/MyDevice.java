/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.repos.DeviceRepo;

/** Singleton - immutable representing our device */
public class MyDevice {
  // OK, because mAppContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static MyDevice sInstance;

  /** Global Application Context */
  private final Context mAppContext;

  private MyDevice(@NonNull Context context) {
    mAppContext = context.getApplicationContext();
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
    return Prefs.INST(mAppContext).getSN();
  }

  @SuppressWarnings("SameReturnValue")
  public String getOS() {
    return "Android";
  }

  public String getNickname() {
    return Prefs.INST(mAppContext).getDeviceNickname();
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

  /** Notify listeners that our Device was removed */
  public void notifyRemoved() {
    DeviceRepo.INST(App.INST()).removeAll();
    sendBroadcast(Intents.TYPE_MY_DEVICE_REMOVED, "", "");
  }

  /** Notify listeners that our Device was registered */
  public void notifyRegistered() {
    sendBroadcast(Intents.TYPE_MY_DEVICE_REGISTERED, "", "");
  }

  /** Notify listeners that our Device was unregistered */
  public void notifyUnregistered() {
    DeviceRepo.INST(App.INST()).removeAll();
    sendBroadcast(Intents.TYPE_MY_DEVICE_UNREGISTERED, "", "");
  }

  /**
   * Notify listeners that registration failed
   * @param message error message
   */
  public void notifyRegisterError(String message) {
    DeviceRepo.INST(App.INST()).removeAll();
    sendBroadcast(Intents.TYPE_MY_DEVICE_REGISTER_ERROR, Intents.EXTRA_TEXT,
      message);
  }

  /**
   * Broadcast changes to listeners
   * @param action     the type of the change
   * @param extra      extra String info type
   * @param extraValue value of extra
   */
  private void sendBroadcast(String action, String extra, String extraValue) {
    final Intent intent = new Intent(Intents.FILTER_MY_DEVICE);
    final Bundle bundle = new Bundle();
    bundle.putString(Intents.ACTION_TYPE_MY_DEVICE, action);
    if (!TextUtils.isEmpty(extra)) {
      bundle.putString(extra, extraValue);
    }
    intent.putExtra(Intents.BUNDLE_MY_DEVICE, bundle);
    LocalBroadcastManager
      .getInstance(mAppContext)
      .sendBroadcast(intent);
  }
}
