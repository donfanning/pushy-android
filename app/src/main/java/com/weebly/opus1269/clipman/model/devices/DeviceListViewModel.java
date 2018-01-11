/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model.devices;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * TODO: Add description.
 */
public class DeviceListViewModel extends AndroidViewModel {
  private DeviceDatabase mDeviceDatabase;

  private final LiveData<List<Device>> mDeviceList;

  public DeviceListViewModel(@NonNull Application app) {
    super(app);

    mDeviceDatabase = DeviceDatabase.INST(app.getApplicationContext());

    mDeviceList = mDeviceDatabase.deviceDao().getAll();
  }

  public @NonNull LiveData<List<Device>> getDeviceList() {
    return mDeviceList;
  }
}
