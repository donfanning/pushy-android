/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;
import com.weebly.opus1269.clipman.viewmodel.LabelCreateViewModel;

/** Handlers for UI events */
public class LabelCreateHandlers extends BaseHandlers {

  LabelCreateHandlers() {
    super();
  }

  /**
   * Click on create button
   * @param vm A ViewModel
   */
  public void onCreateClick(LabelCreateViewModel vm) {
    if (!AppUtils.isWhitespace(vm.name.getValue())) {
      Analytics.INST(vm.getApplication()).imageClick(TAG, "addLabel");
      vm.create();
      vm.name.setValue("");
    }
  }
}
