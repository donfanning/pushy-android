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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.db.DeviceDB;
import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.msg.MessagingClient;

import java.util.ArrayList;
import java.util.List;

/** Singleton - Repository for {@link Device} objects */
public class DevicesRepo {
  @SuppressLint("StaticFieldLeak")
  private static DevicesRepo sInstance;

  /** Application */
  private final Application mApp;

  /** Database */
  private final DeviceDB mDB;

  /** Info message */
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

    resetInfoMessage();

    setupDevicesBroadcastReceiver();
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

  public LiveData<String> getInfoMessage() {
    return infoMessage;
  }

  private void setInfoMessage(String msg) {
    infoMessage.setValue(msg);
  }

  public LiveData<List<DeviceEntity>> getDeviceList() {
    // TODO why?
    if (deviceList.getValue() == null) {
      deviceList.setValue(new ArrayList<>());
    }
    return deviceList;
  }

  public void ping() {
    MessagingClient.INST(mApp).sendPing();
  }

  public void add(DeviceEntity device) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.deviceDao().insertAll(device));
  }

  public void remove(DeviceEntity device) {
    App.getExecutors().diskIO().execute(() -> mDB.deviceDao().delete(device));
  }

  public void removeAll() {
    App.getExecutors().diskIO().execute(() -> mDB.deviceDao().deleteAll());
  }

  private void resetInfoMessage() {
    if (!Prefs.INST(mApp).isPushClipboard()) {
      setInfoMessage(mApp.getString(R.string.err_no_push_to_devices));
    } else if (!Prefs.INST(mApp).isAllowReceive()) {
      setInfoMessage(mApp.getString(R.string.err_no_receive_from_devices));
    } else {
      setInfoMessage("");
    }
  }

  /** Create the {@link BroadcastReceiver} to handle changes to the list */
  private void setupDevicesBroadcastReceiver() {
    // handler for received Intents for the "devices" event
    final BroadcastReceiver receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        final Bundle bundle = intent.getBundleExtra(Intents.BUNDLE_DEVICES);
        if (bundle == null) {
          return;
        }
        final String action = bundle.getString(Intents.ACTION_TYPE_DEVICES);
        if (action == null) {
          return;
        }

        switch (action) {
          case Intents.TYPE_NO_REMOTE_DEVICES:
            setInfoMessage(mApp.getString(R.string.err_no_remote_devices));
            break;
          default:
            break;
        }
      }
    };

    // Register mReceiver to receive Device notifications.
    LocalBroadcastManager.getInstance(mApp)
      .registerReceiver(receiver, new IntentFilter(Intents.FILTER_DEVICES));
  }
}
