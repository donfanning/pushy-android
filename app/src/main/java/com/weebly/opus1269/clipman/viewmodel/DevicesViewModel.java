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
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.repos.DevicesRepo;

import java.util.List;


/** ViewModel for a collection of Devices */
public class DevicesViewModel extends AndroidViewModel {
  /** Device Repo */
  private final DevicesRepo mRepo;

  public DevicesViewModel(@NonNull Application app) {
    super(app);

    mRepo = DevicesRepo.INST(app);
    updateList();
  }

  public LiveData<String> getInfoMessage() {
    return mRepo.getInfoMessage();
  }

  public LiveData<List<DeviceEntity>> getDeviceList() {
    return mRepo.getDeviceList();
  }

  public void updateList() {
    mRepo.ping();
  }
}
