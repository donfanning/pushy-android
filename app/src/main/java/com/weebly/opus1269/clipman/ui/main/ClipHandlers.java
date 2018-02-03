/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;
import com.weebly.opus1269.clipman.viewmodel.ClipViewModel;
import com.weebly.opus1269.clipman.viewmodel.MainViewModel;

/** Handlers for UI events */
public class ClipHandlers extends BaseHandlers {
  private final MainActivity mActivity;
  private final String TAG;

  ClipHandlers(MainActivity activity) {
    super();
    this.mActivity = activity;
    this.TAG = activity.getTAG();
  }

  /**
   * Click on fab button
   * @param fab The View
   * @param vm  The ViewModel
   */
  public void onFabClick(View fab, MainViewModel vm) {
    if (vm != null && vm.getSelectedClipSync() != null) {
      vm.getSelectedClipSync().doShare(vm.getApplication(), fab);
      Analytics.INST(vm.getApplication()).imageClick(TAG, "clipItemShare");
    }
  }

  /**
   * Click on Clip item
   * @param vm The ViewModel
   */
  public void onItemClick(ClipViewModel vm) {
    final ClipEntity clipEntity = vm.getClip().getValue();
    Analytics.INST(mActivity).click(TAG, "clipItemRow");
    mActivity.startOrUpdateClipViewer(clipEntity);
  }

  /**
   * Click on copy button
   * @param clipEntity The Clip
   * @param view       The View
   */
  public void onCopyClick(ClipEntity clipEntity, ImageView view) {
    final Context context = view.getContext();
    Analytics.INST(context).imageClick(TAG, "clipItemCopy");
    clipEntity.setRemote(false);
    clipEntity.copyToClipboard(context);
    if (!Prefs.INST(context).isMonitorClipboard()) {
      AppUtils.showMessage(mActivity, mActivity.getFab(),
        context.getString(R.string.clipboard_copy));
    }
  }

  /**
   * Click on labels button
   * @param clipEntity The Clip
   * @param view       The View
   */
  public void onLabelsClick(ClipEntity clipEntity, ImageView view) {
    final Context context = view.getContext();
    Log.logD(TAG, "select labels clicked");
    Analytics.INST(context).imageClick(TAG, "clipItemLabels");
    final Intent intent = new Intent(mActivity, LabelsSelectActivity.class);
    intent.putExtra(Intents.EXTRA_CLIP, clipEntity);
    AppUtils.startActivity(mActivity, intent);
  }

  /**
   * Click on fav checkbox
   * @param vm       The ViewModel
   * @param checkBox The CheckBox
   */
  public void onFavClick(ClipViewModel vm, CheckBox checkBox) {
    final Context context = checkBox.getContext();
    final boolean checked = checkBox.isChecked();
    vm.changeFav(checked);
    Analytics.INST(context).checkBoxClick(TAG, "clipItemFav: " + checked);
  }
}
