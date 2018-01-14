/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.model.Notifications;

/** Activity to manage our connected devices */
public class DevicesActivity extends BaseActivity {

  /** Adapter being used to display the list's data */
  private DevicesAdapter mAdapter = null;

  /** Receiver to be notified of changes */
  private BroadcastReceiver mReceiver = null;

  /** Info. message if list is not visible */
  private String mInfoMessage = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_devices;

    super.onCreate(savedInstanceState);

    final FloatingActionButton fab = findViewById(R.id.fab);
    if (fab != null) {
      fab.setOnClickListener(v -> {
        setupMainView();
        final Snackbar snack =
          Snackbar.make(fab, getString(R.string.ping_message), 5000);
        snack.show();
        ping();
        Analytics.INST(v.getContext()).imageClick(TAG, "refreshDevices");
      });
    }

    setupRecyclerView();

    setupDevicesBroadcastReceiver();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Register mReceiver to receive Device notifications.
    LocalBroadcastManager.getInstance(this)
      .registerReceiver(mReceiver, new IntentFilter(Intents.FILTER_DEVICES));

    // remove any displayed device notifications
    Notifications.INST(this).removeDevices();

    // show list or info. message
    setupMainView();

    // ping devices
    ping();
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Unregister since the activity is not visible
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
  }

  /** Connect the {@link DevicesAdapter} to the {@link RecyclerView} */
  private void setupRecyclerView() {
    final RecyclerView recyclerView = findViewById(R.id.deviceList);
    if (recyclerView != null) {
      mAdapter = new DevicesAdapter(this);
      recyclerView.setAdapter(mAdapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
  }

  /** Determine if list or error should be shown */
  private void setupMainView() {

    final RecyclerView recyclerView = findViewById(R.id.deviceList);
    final TextView textView = findViewById(R.id.info_message);
    final FloatingActionButton fab = findViewById(R.id.fab);

    if (!Prefs.INST(getApplicationContext()).isPushClipboard()) {
      mInfoMessage = getString(R.string.err_no_push_to_devices);
    } else if (!Prefs.INST(getApplicationContext()).isAllowReceive()) {
      mInfoMessage = getString(R.string.err_no_receive_from_devices);
    }

    if (TextUtils.isEmpty(mInfoMessage)) {
      refreshList();

      textView.setVisibility(View.GONE);
      recyclerView.setVisibility(View.VISIBLE);
      fab.setVisibility(View.VISIBLE);
      textView.setText("");
    } else {
      textView.setText(mInfoMessage);
      textView.setVisibility(View.VISIBLE);
      recyclerView.setVisibility(View.GONE);
      fab.setVisibility(View.GONE);
    }
  }

  /** Create the {@link BroadcastReceiver} to handle changes to the list */
  private void setupDevicesBroadcastReceiver() {
    // handler for received Intents for the "devices" event
    mReceiver = new BroadcastReceiver() {
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
            if (Devices.INST(getApplicationContext()).getCount() > 0) {
              mInfoMessage = "";
            }
            setupMainView();
            break;
          case Intents.TYPE_NO_REMOTE_DEVICES:
            // detected no remote devices - don't ping here
            mInfoMessage = getString(R.string.err_no_remote_devices);
            setupMainView();
            break;
          default:
            break;
        }
      }
    };
  }

  /** Ping our devices */
  private void ping() {
    MessagingClient.INST(this).sendPing();
  }

  /** Refresh the list */
  private void refreshList() {
    mAdapter.notifyDataSetChanged();
  }
}
