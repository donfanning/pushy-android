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
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.db.DeviceDB;
import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.msg.MessagingClient;

import java.util.ArrayList;
import java.util.List;

/** Singleton - Repository for {@link Device} objects */
public class DevicesRepo implements
  SharedPreferences.OnSharedPreferenceChangeListener {
  @SuppressLint("StaticFieldLeak")
  private static DevicesRepo sInstance;

  /** Application */
  private final Application mApp;

  /** Database */
  private final DeviceDB mDB;

  /** Info message - not saved in database */
  private final MutableLiveData<String> infoMessage;

  /** Device List */
  private final MediatorLiveData<List<DeviceEntity>> deviceList;

  private DevicesRepo(final Application app) {
    mApp = app;
    mDB = DeviceDB.INST(app);

    infoMessage = new MutableLiveData<>();
    deviceList = new MediatorLiveData<>();

    deviceList.addSource(mDB.deviceDao().getAll(), devices -> {
      if (mDB.getDatabaseCreated().getValue() != null) {
        deviceList.postValue(devices);
      }
    });

    initInfoMessage();

    // listen for shared preference changes
    PreferenceManager
      .getDefaultSharedPreferences(mApp)
      .registerOnSharedPreferenceChangeListener(this);
  }

  public static DevicesRepo INST(final Application app) {
    if (sInstance == null) {
      synchronized (DevicesRepo.class) {
        if (sInstance == null) {
          sInstance = new DevicesRepo(app);
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

  public LiveData<String> getInfoMessage() {
    return infoMessage;
  }

  private void setInfoMessage(String msg) {
    infoMessage.setValue(msg);
  }

  private void postInfoMessage(String msg) {
    infoMessage.postValue(msg);
  }

  public LiveData<List<DeviceEntity>> getDeviceList() {
    if (deviceList.getValue() == null) {
      // if no items in database, List will be null
      deviceList.setValue(new ArrayList<>());
    }
    return deviceList;
  }

  /** Send ping to query remote devices */
  public void refreshList() {
    MessagingClient.INST(mApp).sendPing();
  }

  /** Detected that the server has no other registered devices */
  public void noDevices() {
    final String msg = mApp.getString(R.string.err_no_remote_devices);
    postInfoMessage(msg);
    removeAll();
  }

  public void add(DeviceEntity device) {
    App.getExecutors().diskIO().execute(() -> {
      mDB.deviceDao().insertAll(device);
      final String msg = mApp.getString(R.string.err_no_remote_devices);
      if(msg.equals(getInfoMessage().getValue())) {
        // clear no remote device message when we add new device
        postInfoMessage("");
      }
    });
  }

  public void remove(DeviceEntity device) {
    App.getExecutors().diskIO().execute(() -> mDB.deviceDao().delete(device));
  }

  public void removeAll() {
    App.getExecutors().diskIO().execute(() -> mDB.deviceDao().deleteAll());
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
