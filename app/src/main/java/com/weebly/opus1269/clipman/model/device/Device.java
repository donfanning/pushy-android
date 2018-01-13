/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model.device;

/** Interface for a hardware device */
public interface Device {
  String getModel();
  String getSN();
  String getOS();
  String getNickname();
  long getLastSeen();
  String getDisplayName();
  String getUniqueName();
}