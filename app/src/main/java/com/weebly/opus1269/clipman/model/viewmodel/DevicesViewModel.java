/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.device.Device;

import java.util.ArrayList;
import java.util.List;

/** View Model for Devices Activity */
public class DevicesViewModel extends AndroidViewModel {
  /** Application context */
  @SuppressLint("StaticFieldLeak")
  final private Application mApp;

  /** Class Indentifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Info message */
  private MutableLiveData<String> infoMessage = new MutableLiveData<>();

  /** Device List */
  private MutableLiveData<List<Device>> deviceList = new MutableLiveData<>();

  public DevicesViewModel(@NonNull Application app) {
    super(app);

    this.mApp = app;

    resetInfoMessage();

    loadList();

    setupDevicesBroadcastReceiver();
  }

  public String getInfoMessage() {
    return infoMessage.getValue();
  }

  private void setInfoMessage(String msg) {
    infoMessage.setValue(msg);
  }

  public LiveData<List<Device>> getDeviceList() {
    return deviceList;
  }

  private void loadList() {
    deviceList.setValue(new ArrayList<>());
    List<Device> devices = deviceList.getValue();
    if (devices != null) {
      devices.addAll(Devices.INST(mApp).getList());
    }
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
          case Intents.TYPE_UPDATE_DEVICES:
            // device list changed - don't ping here
            if (Devices.INST(mApp).getCount() > 0) {
              setInfoMessage("");
            } else {
              resetInfoMessage();
            }
            loadList();
            break;
          case Intents.TYPE_NO_REMOTE_DEVICES:
            // detected no remote devices - don't ping here
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
