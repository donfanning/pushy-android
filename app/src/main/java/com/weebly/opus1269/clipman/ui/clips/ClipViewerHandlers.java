/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clips;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;
import com.weebly.opus1269.clipman.viewmodel.ClipViewerFragViewModel;
import com.weebly.opus1269.clipman.viewmodel.ClipViewerViewModel;

/** Handlers for UI events */
public class ClipViewerHandlers extends BaseHandlers {

  ClipViewerHandlers() {
    super();
  }

  /**
   * Click on fab button
   * @param fab The View
   * @param vm  The ViewModel
   */
  public void onFabClick(View fab, @NonNull ClipViewerViewModel vm) {
    Log.logD(TAG, "fab clicked");
    final ClipEntity clipEntity = vm.getClipSync();
    if (clipEntity != null) {
      final Context context = fab.getContext();
      clipEntity.doShare(context, fab);
      Analytics.INST(context).imageClick(TAG, "clipItemShare");
    }
  }

  /**
   * Click on labels
   * @param vm The ViewModel
   */
  public void onLabelsClick(@NonNull ClipViewerFragViewModel vm) {
    final Context context = vm.getApplication();
    Analytics.INST(context).click(TAG, "showLabelList");
    final Intent intent = new Intent(context, LabelsSelectActivity.class);
    intent.putExtra(Intents.EXTRA_CLIP, vm.getClipSync());
    AppUtils.startActivity(context, intent);
  }
}
