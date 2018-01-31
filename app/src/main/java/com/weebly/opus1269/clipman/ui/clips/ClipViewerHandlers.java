/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clips;

import android.content.Context;
import android.content.Intent;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;
import com.weebly.opus1269.clipman.viewmodel.ClipViewerViewModel;

/** Handlers for UI events */
public class ClipViewerHandlers extends BaseHandlers {

  ClipViewerHandlers() {
    super();
  }

  /**
   * Click on labels
   * @param context A Context
   * @param vm  The ViewModel
   */
  public void onLabelsClick(Context context, ClipViewerViewModel vm) {
    if (vm != null) {
      Analytics.INST(context).click(TAG, "showLabelList");
      final Intent intent = new Intent(context, LabelsSelectActivity.class);
      intent.putExtra(Intents.EXTRA_CLIP, vm.getClip());
      AppUtils.startActivity(context, intent);
    }
  }
}
