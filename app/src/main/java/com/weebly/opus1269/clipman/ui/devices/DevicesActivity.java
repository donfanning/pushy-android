/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.devices;

import android.os.Bundle;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.databinding.DevicesBinding;
import com.weebly.opus1269.clipman.viewmodel.DevicesViewModel;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.model.Notifications;

/** Activity to manage our connected devices */
public class DevicesActivity extends BaseActivity<DevicesBinding> {
  private DevicesAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_devices;
    mIsBound = true;

    super.onCreate(savedInstanceState);

    // setup ViewModel and data binding
    final DevicesViewModel vm = new DevicesViewModel(getApplication());
    final DeviceHandlers handlers = new DeviceHandlers(getTAG());
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(vm);
    mBinding.setInfoMessage(vm.getInfoMessage());
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

    // setup RecyclerView
    mAdapter = new DevicesAdapter(this, handlers);
    mBinding.content.recycler.setAdapter(mAdapter);

    // Observe devices
    vm.getDevices().observe(this, devices -> mAdapter.setList(devices));
  }

  @Override
  protected void onResume() {
    super.onResume();

    // remove any displayed device notifications
    Notifications.INST(this).removeDevices();
  }
}
