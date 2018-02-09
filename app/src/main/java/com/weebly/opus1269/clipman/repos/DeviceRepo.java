/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.repos;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.DeviceDB;
import com.weebly.opus1269.clipman.db.entity.Device;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.msg.MessagingClient;

import java.util.ArrayList;
import java.util.List;

/** Singleton - Repository for {@link Device} objects */
public class DeviceRepo extends BaseRepo implements
  SharedPreferences.OnSharedPreferenceChangeListener {
  @SuppressLint("StaticFieldLeak")
  private static DeviceRepo sInstance;

  /** Error message for no remote device */
  private final String ERR_NO_REMOTE_DEVICES;

  /** Database */
  private final DeviceDB mDB;

  /** Device List */
  private final MediatorLiveData<List<Device>> deviceList;

  private DeviceRepo(final Application app) {
    super(app);

    mDB = DeviceDB.INST(app);

    ERR_NO_REMOTE_DEVICES = mApp.getString(R.string.err_no_remote_devices);

    deviceList = new MediatorLiveData<>();
    deviceList.addSource(mDB.deviceDao().getAll(), devices -> {
      if (mDB.getDatabaseCreated().getValue() != null) {
        deviceList.postValue(devices);
        if(!AppUtils.isEmpty(devices)) {
          if (ERR_NO_REMOTE_DEVICES.equals(getInfoMessage().getValue())) {
            initInfoMessage();
          }
        }
      }
    });

    initInfoMessage();

    // listen for shared preference changes
    PreferenceManager.getDefaultSharedPreferences(mApp)
      .registerOnSharedPreferenceChangeListener(this);
  }

  public static DeviceRepo INST(final Application app) {
    if (sInstance == null) {
      synchronized (DeviceRepo.class) {
        if (sInstance == null) {
          sInstance = new DeviceRepo(app);
        }
      }
    }
    return sInstance;
  }

  @Override
  public void
  onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    final String keyPush = mApp.getString(R.string.key_pref_push_msg);
    final String keyReceive = mApp.getString(R.string.key_pref_receive_msg);

    if (key.equals(keyPush)) {
      final String msg = Prefs.INST(mApp).isPushClipboard() ?
        "" : mApp.getString(R.string.err_no_push_to_devices);
      postInfoMessage(msg);
    } else if (key.equals(keyReceive)) {
      final String msg = Prefs.INST(mApp).isAllowReceive() ?
        "" : mApp.getString(R.string.err_no_receive_from_devices);
      postInfoMessage(msg);
    }
  }

  @NonNull public LiveData<List<Device>> getDevices() {
    if (deviceList.getValue() == null) {
      // if no items in database, List will be null
      deviceList.setValue(new ArrayList<>());
    }
    return deviceList;
  }

  public void add(Device device) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.deviceDao().insert(device));
  }

  public void remove(Device device) {
    App.getExecutors().diskIO().execute(() -> mDB.deviceDao().delete(device));
  }

  public void removeAll() {
    App.getExecutors().diskIO().execute(() -> mDB.deviceDao().deleteAll());
  }


  /** Send ping to query remote devices */
  public void refreshList() {
    MessagingClient.INST(mApp).sendPing();
  }

  /** Detected that the server has no other registered devices */
  public void noRegisteredDevices() {
    postInfoMessage(ERR_NO_REMOTE_DEVICES);
    removeAll();
  }

  private void initInfoMessage() {
    if (!Prefs.INST(mApp).isPushClipboard()) {
      postInfoMessage(mApp.getString(R.string.err_no_push_to_devices));
    } else if (!Prefs.INST(mApp).isAllowReceive()) {
      postInfoMessage(mApp.getString(R.string.err_no_receive_from_devices));
    } else {
      postInfoMessage("");
    }
  }
}
