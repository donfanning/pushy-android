/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * A descriptive label for an item
 */
public class Label implements Serializable {
  private static final String TAG = "Label";

  /** The name of the label */
  @NonNull private String mName;

  public Label() {
    mName = "Label name";
  }

  public Label(@NonNull String name) {
    mName = name;
  }

  @NonNull public String getName() {
    return mName;
  }

  public void setName(@NonNull String name) {
    mName = name;
  }
}
