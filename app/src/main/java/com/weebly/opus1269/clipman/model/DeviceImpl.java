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

import android.text.TextUtils;

import org.threeten.bp.Instant;

/**
 * Immutable Class that represents a (hopefully) unique hardware device
 * Emulators don't have good names and serial numbers
 */
public class DeviceImpl implements Device {
  // IMPORTANT Only use primitives because we go to Preferences as a list of
  // devices

  // These three should be unique (not for emulators)
  private final String model;
  private final String sn;
  private final String os;

  private final String nickname;
  private final long lastSeen;

  public DeviceImpl(String model, String sn, String os, String nickname) {
    this.model = model;
    this.sn = sn;
    this.os = os;
    this.nickname = nickname;
    this.lastSeen = Instant.now().toEpochMilli();
  }

  public String getModel() {
    return model;
  }

  public String getSN() {
    return sn;
  }

  public String getOS() {
    return os;
  }

  public String getNickname() {
    return nickname;
  }

  public long getLastSeen() {return lastSeen;}

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
