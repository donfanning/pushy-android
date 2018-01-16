/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.model.device.Device;
import com.weebly.opus1269.clipman.repos.DevicesRepo;

/** ViewModel for a {@link Device} */
public class DeviceViewModel extends AndroidViewModel {
  /** Device Repo */
  private final DevicesRepo mRepo;

  /** Our Device */
  private final DeviceEntity device;

  public DeviceViewModel(@NonNull Application app, DeviceEntity device) {
    super(app);

    this.device = device;
    mRepo = DevicesRepo.INST(app);
  }

  public Device getDevice() {
    return device;
  }

  public void remove() {
    mRepo.remove(device);
  }
}
