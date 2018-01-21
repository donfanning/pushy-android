/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui;

import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/** Data binding utility methods */
public class BindingAdapters {
  /**
   * Show or hide a view
   * @param view The view
   * @param show The state
   */
  @BindingAdapter("visibleGone")
  public static void showHide(View view, boolean show) {
    view.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  /**
   * Set enabled state and show greyed out when disabled
   * @param view The view
   * @param enable The state
   */
  @BindingAdapter("enabled")
  public static void enabled(ImageView view, boolean enable) {
    view.setEnabled(enable);
    view.setImageAlpha(enable ? 255 : 64);
  }

  /**
   * Tint the view with the accent color
   * @param view The view
   * @param isTrue tint if true
   */
  @BindingAdapter("tintAccent")
  public static void tintAccent(ImageView view, boolean isTrue) {
    if (isTrue) {
      DrawableHelper.tintAccentColor(view);
    }
  }

  /**
   * Tint the view with the primary color
   * @param view The view
   * @param noReverse revese color if true
   */
  @BindingAdapter("tintPrimary")
  public static void tintPrimary(ImageView view, boolean noReverse) {
      DrawableHelper.tintPrimaryColor(view, !noReverse);
  }
}
