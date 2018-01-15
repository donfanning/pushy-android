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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weebly.opus1269.clipman.model.device.Device;
import com.weebly.opus1269.clipman.model.device.DeviceImpl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Singleton to manage the collection of registered {@link Device} objects.
 * Register a {@link LocalBroadcastManager} with
 * Intents.FILTER_DEVICES to be notified of changes.
 */
public class Devices {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static Devices sInstance;

  /** Global Application Context */
  private final Context mContext;

  /** {@link Device} list */
  private List<Device> sDevices;

  private Devices(@NonNull Context context) {
    mContext = context.getApplicationContext();
    sDevices = load();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static Devices INST(@NonNull Context context) {
    synchronized (Devices.class) {
      if (sInstance == null) {
        sInstance = new Devices(context);
      }
      return sInstance;
    }
  }

  /**
   * Get the list
   * @return our list
   */
  public List<Device> getList() {
    return sDevices;
  }

  /**
   * Save list to persistant storage
   * @param broadcast broadcast result to listeners if true
   */
  private void save(Boolean broadcast) {
    final Comparator<Device> cmp = (lhs, rhs) -> {
      // newest first
      return ((Long) rhs.getLastSeen())
        .compareTo(lhs.getLastSeen());
    };
    // sort by lastSeen
    Collections.sort(sDevices, cmp);

    // persist
    final Gson gson = new Gson();
    final String devicesString = gson.toJson(sDevices);
    Prefs.INST(mContext).setDevices(devicesString);

    if (broadcast) {
      // let listeners know
      sendBroadcast(Intents.TYPE_UPDATE_DEVICES);
    }
  }

  /**
   * Load list from persistant storage
   * @return List<Device> the list of {@link Device} objects
   */
  private List<Device> load() {
    final String devicesString = Prefs.INST(mContext).getDevices();

    if (devicesString.isEmpty()) {
      sDevices = new ArrayList<>(0);
    } else {
      final Gson gson = new Gson();
      final Type type = new TypeToken<ArrayList<DeviceImpl>>() {
      }.getType();
      sDevices = gson.fromJson(devicesString, type);
    }
    return sDevices;
  }

  /**
   * Add or update {@link Device} as needed. Broadcast change if requested.
   * @param dev       The {@link Device} to add or update
   * @param broadcast broadcast result to listeners if true
   */
  public void add(Device dev, Boolean broadcast) {
    if (dev != null) {
      int i = 0;
      for (final Device device : sDevices) {
        if (dev.getUniqueName().equals(device.getUniqueName())) {
          // found, nickname or lastSeen probably changed,
          // update device
          sDevices.set(i, dev);
          save(broadcast);
          return;
        }
        i++;
      }
      sDevices.add(dev);
      save(broadcast);
    }
  }

  /**
   * Remove the given {@link Device}
   * @param dev The {@link Device} to remove
   */
  public void remove(Device dev) {
    if (dev != null) {
      for (final Iterator<Device> i = sDevices.iterator(); i.hasNext(); ) {
        final Device device = i.next();
        if (dev.getUniqueName().equals(device.getUniqueName())) {
          i.remove();
          save(true);
          break;
        }
      }
    }
  }

  /**
   * Remove all devices
   */
  void clear() {
    sDevices.clear();
    save(true);
  }

  /**
   * Notify listeners that our {@link Device} was removed
   */
  public void notifyMyDeviceRemoved() {
    clear();
    sendBroadcast(Intents.TYPE_OUR_DEVICE_REMOVED);
  }

  /**
   * Notify listeners that our {@link Device} was registered
   */
  public void notifyMyDeviceRegistered() {
    sendBroadcast(Intents.TYPE_OUR_DEVICE_REGISTERED);
  }

  /**
   * Notify listeners that our {@link Device} was unregistered
   */
  public void notifyMyDeviceUnregistered() {
    clear();
    sendBroadcast(Intents.TYPE_OUR_DEVICE_UNREGISTERED);
  }

  /**
   * Notify listeners that registration failed
   * @param message error message
   */
  public void notifyMyDeviceRegisterError(String message) {
    clear();
    sendBroadcast(Intents.TYPE_OUR_DEVICE_REGISTER_ERROR, Intents.EXTRA_TEXT,
      message);
  }

  /**
   * Notify listeners that no remote devices are registered
   */
  public void notifyNoRemoteDevicesError() {
    clear();
    sendBroadcast(Intents.TYPE_NO_REMOTE_DEVICES, "", "");
  }


  /**
   * Get the {@link Device} at the given position
   * @param pos Position in list
   * @return A {@link Device}
   */
  public Device get(int pos) {
    return sDevices.get(pos);
  }

  /**
   * Get the number of {@link Device} objects in the list
   * @return the number of {@link Device} objects in the list
   */
  public int getCount() {
    return sDevices.size();
  }

  /**
   * Broadcast changes to listeners
   * @param action     the type of the change
   * @param extra      extra String info type
   * @param extraValue value of extra
   */
  private void sendBroadcast(String action, String extra, String extraValue) {
    final Intent intent = new Intent(Intents.FILTER_DEVICES);
    final Bundle bundle = new Bundle();
    bundle.putString(Intents.ACTION_TYPE_DEVICES, action);
    if (!TextUtils.isEmpty(extra)) {
      bundle.putString(extra, extraValue);
    }
    intent.putExtra(Intents.BUNDLE_DEVICES, bundle);
    LocalBroadcastManager
      .getInstance(mContext)
      .sendBroadcast(intent);
  }

  /**
   * Broadcast changes to listeners
   * @param action the type of the change
   */
  private void sendBroadcast(String action) {
    sendBroadcast(action, "", "");
  }
}
