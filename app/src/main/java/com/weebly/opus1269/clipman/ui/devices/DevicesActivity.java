/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.devices;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.databinding.ActivityDevicesBinding;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.viewmodel.DevicesViewModel;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.model.Notifications;

/** Activity to manage our connected devices */
public class DevicesActivity extends BaseActivity {
  /** The ViewModel */
  private DevicesViewModel vm;

  /** Adapter being used to display the list's data */
  private DevicesAdapter mAdapter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_devices;
    mIsbound = true;

    super.onCreate(savedInstanceState);

    // setup ViewModel and data binding
    vm = new DevicesViewModel(getApplication());
    ((ActivityDevicesBinding) mBinding).setVm(vm);

    final FloatingActionButton fab = findViewById(R.id.fab);
    if (fab != null) {
      fab.setOnClickListener(v -> {
        refreshList();
        final Snackbar snack =
          Snackbar.make(fab, getString(R.string.ping_message), 5000);
        snack.show();
        ping();
        Analytics.INST(v.getContext()).imageClick(TAG, "refreshDevices");
      });
    }

    setupRecyclerView();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // remove any displayed device notifications
    Notifications.INST(this).removeDevices();

    // force update of adapter
    refreshList();

    // ping devices
    ping();
  }

  /** Connect the {@link DevicesAdapter} to the {@link RecyclerView} */
  private void setupRecyclerView() {
    final RecyclerView recyclerView = findViewById(R.id.deviceList);
    if (recyclerView != null) {
      mAdapter = new DevicesAdapter(this, vm.getDeviceList());
      recyclerView.setAdapter(mAdapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
  }

  /** Ping our devices */
  private void ping() {
    MessagingClient.INST(this).sendPing();
  }

  /** Refresh the list */
  private void refreshList() {
    if (TextUtils.isEmpty(vm.getInfoMessage())) {
      mAdapter.notifyDataSetChanged();
    }
  }
}
