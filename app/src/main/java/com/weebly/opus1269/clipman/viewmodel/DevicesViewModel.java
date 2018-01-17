/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.repos.DevicesRepo;

import java.util.List;

/** ViewModel for a collection of Devices */
public class DevicesViewModel extends AndroidViewModel {
  /** Device Repo */
  private final DevicesRepo mRepo;

  // MediatorLiveData can observe other
  // LiveData objects and react on their emissions.
  /** Device list */
  private final MediatorLiveData<List<DeviceEntity>> devices;

  /** Info message */
  private final MediatorLiveData<String> infoMessage;


  public DevicesViewModel(@NonNull Application app) {
    super(app);

    mRepo = DevicesRepo.INST(app);

    devices = new MediatorLiveData<>();
    // set by default null, until we get data from the repo.
    devices.setValue(null);
    // observe the changes of the devices from the repo and forward them
    devices.addSource(mRepo.getDeviceList(), devices::setValue);

    infoMessage = new MediatorLiveData<>();
    infoMessage.setValue(mRepo.getInfoMessage().getValue());
    infoMessage.addSource(mRepo.getInfoMessage(), infoMessage::setValue);

    // ping devices
    refreshList();
  }

  public LiveData<List<DeviceEntity>> getDevices() {
    return devices;
  }

  public LiveData<String> getInfoMessage() {
    return infoMessage;
  }

  public void refreshList() {
    mRepo.refreshList();
  }
}
