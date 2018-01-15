/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.devices;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.databinding.ActivityDevicesBinding;
import com.weebly.opus1269.clipman.model.viewmodel.DevicesViewModel;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.model.Notifications;

/** Activity to manage our connected devices */
public class DevicesActivity extends BaseActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_devices;
    mIsbound = true;

    super.onCreate(savedInstanceState);

    // setup ViewModel and data binding
    final DevicesViewModel vm = new DevicesViewModel(getApplication());
    final DevicesHandlers handlers = new DevicesHandlers(getTAG());
    final ActivityDevicesBinding binding = (ActivityDevicesBinding) mBinding;
    binding.setVm(vm);
    binding.setHandlers(handlers);

    // setup RecyclerView
    final RecyclerView recyclerView = findViewById(R.id.deviceList);
    if (recyclerView != null) {
      final DevicesAdapter adapter =
        new DevicesAdapter(this, handlers, vm.getDeviceList());
      recyclerView.setAdapter(adapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // remove any displayed device notifications
    Notifications.INST(this).removeDevices();
  }
}
