/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.devices;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.device.Device;

/** Handler for UI events */
public class DevicesHandlers {
  private final DevicesActivity mActivity;

  DevicesHandlers (DevicesActivity activity) {
    mActivity = activity;
  }

  /** Click on fab button */
  public void onFabClick() {
    final FloatingActionButton fab = mActivity.findViewById(R.id.fab);
    if (fab != null) {
        mActivity.refreshList();
        final Snackbar snack =
          Snackbar.make(fab, mActivity.getString(R.string.ping_message), 5000);
        snack.show();
        mActivity.ping();
        Analytics.INST(fab.getContext()).imageClick(mActivity.getTAG(), "refreshDevices");
    }
  }

  /**
   * Click on forget button of a device
   * @param device The device
   */
  public void onForgetClick(Device device) {
    Devices.INST(mActivity).remove(device);
    Analytics.INST(mActivity)
      .imageClick("DevicesActivity", "removeDevice");
  }

}
