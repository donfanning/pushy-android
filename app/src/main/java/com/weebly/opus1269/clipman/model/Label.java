/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import java.io.Serializable;

/**
 * A descriptive label for an item
 */
public class Label implements Serializable {
  private static final String TAG = "Label";

  /** The name of the label */
  private String mName;

  public Label() {
    mName = "Label name";
  }

  public Label(String name) {
    mName = name;
  }

  public String getName() {
    return mName;
  }

  public void setName(String name) {
    mName = name;
  }
}