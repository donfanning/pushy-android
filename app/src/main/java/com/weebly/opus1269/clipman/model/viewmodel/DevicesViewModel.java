/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.model.device.Device;
import com.weebly.opus1269.clipman.repos.DevicesRepo;

import java.util.List;

/** View Model for Devices Activity */
public class DevicesViewModel extends AndroidViewModel {
  /** Device Repo */
  private final DevicesRepo mRepo;

  public DevicesViewModel(@NonNull Application app) {
    super(app);

    mRepo = new DevicesRepo(app);
  }

  public LiveData<String> getInfoMessage() {
    return mRepo.getInfoMessage();
  }

  public LiveData<List<Device>> getDeviceList() {
    return mRepo.getDeviceList();
  }

  public void updateList() {
    mRepo.updateList();
  }

  public void removeDevice(Device device) {
    mRepo.removeDevice(device);
  }
}
