/*
 *
 * Copyright 2016 Michael A Updike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.weebly.opus1269.clipman.model;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.AppUtils;

import org.threeten.bp.Instant;

/**
 * Immutable Class that represents a (hopefully) unique device
 * Emulators don't have good names and serial numbers
 */
public class Device {
  // IMPORTANT Only use primitives because we go to Preferences as a list of
  // devices

  // These three should be unique (not for emulators)
  private final String mModel;
  private final String mSN;
  private final String mOS;

  private final String mNickname;
  private final long mLastSeen;

  public Device(String model, String sn, String os, String nickname) {
    mModel = model;
    mSN = sn;
    mOS = os;
    mNickname = nickname;
    mLastSeen = Instant.now().toEpochMilli();
  }

  public static Device getMyDevice(Context context) {
    return new Device(getMyModel(), getMySN(context), getMyOS(),
      Prefs.INST(context).getDeviceNickname());
  }

  public static String getMyName(Context context) {
    String myName = getMyNickname(context);
    if (TextUtils.isEmpty(myName)) {
      myName = getMyModel() + " - " + getMySN(context) + " - " + getMyOS();
    }
    return myName;
  }

  public static String getMyModel() {
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

  public static String getMySN(Context context) {
    return Prefs.INST(context).getSN();
  }

  @SuppressWarnings("SameReturnValue")
  @NonNull
  public static String getMyOS() {
    return "Android";
  }

  public static String getMyNickname(Context context) {
    return Prefs.INST(context).getDeviceNickname();
  }

  @NonNull
  public static String getMyUniqueName(Context context) {
    return getMyModel() + " - " + getMySN(context) + " - " + getMyOS();
  }

  public String getModel() {
    return mModel;
  }

  public String getSN() {
    return mSN;
  }

  public String getOS() {
    return mOS;
  }

  public String getNickname() {
    return mNickname;
  }

  public long getLastSeen() {return mLastSeen;}

  public String getDisplayName() {
    String name = getNickname();
    if (TextUtils.isEmpty(name)) {
      name = getModel() + " - " + getSN() + " - " + getOS();
    }
    return name;
  }

  public String getUniqueName() {
    return getModel() + " - " + getSN() + " - " + getOS();
  }
}
