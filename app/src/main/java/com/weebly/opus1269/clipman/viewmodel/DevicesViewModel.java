/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.repos.DeviceRepo;

import java.util.List;

/** ViewModel for a collection of Devices */
public class DevicesViewModel extends BaseRepoViewModel<DeviceRepo> {
  /** Device list */
  private final MediatorLiveData<List<DeviceEntity>> devices;

  public DevicesViewModel(@NonNull Application app) {
    super(app, DeviceRepo.INST(app));

    devices = new MediatorLiveData<>();
    devices.setValue(null);
    devices.addSource(mRepo.getDevices(), devices::setValue);

    // ping devices
    refreshList();
  }

  public LiveData<List<DeviceEntity>> getDevices() {
    return devices;
  }

  public void refreshList() {
    mRepo.refreshList();
  }
}
