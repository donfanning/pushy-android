/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;

import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;

/** Handlers for UI events */
public class LabelSelectHandlers extends BaseHandlers {
  /** Our Activity */
  private final LabelsSelectActivity mActivity;

  /** Our Activity TAG */
  private final String TAG;

  LabelSelectHandlers(LabelsSelectActivity activity) {
    super();
    mActivity = activity;
    TAG = activity.getTAG();
  }

  /**
   * Click on Label Item
   * @param view  The View
   * @param label The Label
   */
  public void onItemClick(View view, @NonNull Label label,
                          @NonNull CheckBox checkBox) {
    checkBox.setChecked(!checkBox.isChecked());
    final boolean checked = checkBox.isChecked();
    mActivity.getVm().addOrRemoveLabel(label, checked);
    Analytics.INST(view.getContext()).click(TAG, "labelRow");
  }

  /**
   * Click on Label checkbox
   * @param view  The View
   * @param label The Label
   */
  public void onCheckBoxClick(View view, @NonNull Label label) {
    final CheckBox checkBox = (CheckBox) view;
    final boolean checked = checkBox.isChecked();
    mActivity.getVm().addOrRemoveLabel(label, checked);
    Analytics.INST(view.getContext())
      .checkBoxClick(TAG, "labelSelect: " + checked);
  }
}
