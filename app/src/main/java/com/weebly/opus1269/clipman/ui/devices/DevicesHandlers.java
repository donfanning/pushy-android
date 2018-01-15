/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.devices;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.device.Device;
import com.weebly.opus1269.clipman.model.viewmodel.DevicesViewModel;

/** Handler for UI events */
public class DevicesHandlers {
  private final String TAG;

  DevicesHandlers(String tag) {
    TAG = tag;
  }

  /**
   * Click on fab button
   * @param fab The View
   */
  public void onFabClick(DevicesViewModel vm,  FloatingActionButton fab) {
    if (fab != null) {
      if (vm != null) {
        vm.updateList();
      }
      final Context context = fab.getContext();
      final Snackbar snack =
        Snackbar.make(fab, context.getString(R.string.ping_message), 5000);
      snack.show();
      Analytics.INST(context).imageClick(TAG, "refreshDevices");
    }
  }

  /**
   * Click on forget button of a device
   * @param context A context
   * @param device  The device
   */
  public void onForgetClick(Context context, Device device) {
    Devices.INST(context).remove(device);
    Analytics.INST(context)
      .imageClick("DevicesActivity", "removeDevice");
  }
}
