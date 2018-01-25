/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.content.Context;

/** Interface for a Clip item */
public interface Clip {
  long getId();

  String getText();

  long getDate();

  boolean getFav();

  boolean getRemote();

  String getDevice();

  void add(Context context);

  void addIfNew(Context context);

  void send(Context context);

  void copyToClipboard(final Context context);
}
