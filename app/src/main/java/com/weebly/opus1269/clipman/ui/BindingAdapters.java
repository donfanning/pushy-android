/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui;

import android.databinding.BindingAdapter;
import android.view.View;

import com.weebly.opus1269.clipman.app.Log;

/** Data binding utility methods */
public class BindingAdapters {
  @BindingAdapter("visibleGone")
  public static void showHide(View view, boolean show) {
    view.setVisibility(show ? View.VISIBLE : View.GONE);
  }
}
