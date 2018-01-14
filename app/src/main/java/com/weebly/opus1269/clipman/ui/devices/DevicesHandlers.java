/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.devices;

import android.content.Context;

import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.device.Device;

/** Handler for UI events */
public class DevicesHandlers {

  /**
   * Click on forget button
   * @param context A context
   * @param device The device
   */
  public void onForgetClick(Context context, Device device) {
    Devices.INST(context).remove(device);
    Analytics.INST(context)
      .imageClick("DevicesActivity", "removeDevice");
  }
}
