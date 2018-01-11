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

package com.weebly.opus1269.clipman.model.devices;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Prefs;

import org.threeten.bp.Instant;

/**
 * Immutable Class that represents a (hopefully) unique device
 * Emulators don't have good names and serial numbers
 */
@Entity(tableName = "devices",
  indices = {@Index(value = {"model", "sn", "os"}, unique = true)})
public class Device {
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  private int id;

  // These three should be unique (not for emulators)
  private final String model;
  private final String sn;
  private final String os;

  private final String nickname;

  @ColumnInfo(name = "last_seen")
  private long lastSeen;

  public Device(String model, String sn, String os, String nickname) {
    this.model = model;
    this.sn = sn;
    this.os = os;
    this.nickname = nickname;
    this.lastSeen = Instant.now().toEpochMilli();
  }

  @NonNull
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

  public int getId() {return id;}

  public void setId(int uid) {
    this.id = uid;
  }

  public String getModel() {
    return model;
  }

  public String getSn() {
    return sn;
  }

  public String getOs() {
    return os;
  }

  public String getNickname() {
    return nickname;
  }

  public long getLastSeen() {return lastSeen;}

  public void setLastSeen(long lastSeen) {
    this.lastSeen = lastSeen;
  }

  public String getDisplayName() {
    String name = getNickname();
    if (TextUtils.isEmpty(name)) {
      name = getModel() + " - " + getSn() + " - " + getOs();
    }
    return name;
  }

  public String getUniqueName() {
    return getModel() + " - " + getSn() + " - " + getOs();
  }
}
