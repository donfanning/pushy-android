/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.content.Context;
import android.content.Intent;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.MyDevice;
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
   * @param vm The ViewModel
   */
  public void onFabClick(MainViewModel vm) {
    if (vm != null) {
      Log.logD(TAG, "fab clicked");
      // TODO
      //mClipItem.doShare(getContext(), v);
      Analytics.INST(mActivity).imageClick(TAG, "shareClipItem");
    }
  }

  /**
   * Click on fav checkbox
   * @param vm The ViewModel
   */
  public void onItemClick(ClipViewModel vm) {
    final ClipEntity clipEntity = vm.getClip().getValue();
    Log.logD(TAG, "item clicked");
    Analytics.INST(mActivity).click(TAG, "clipItemRow");
    mActivity.getAdapter().setSelectedClip(clipEntity);
    mActivity.startOrUpdateClipViewer(clipEntity);
  }

  /**
   * Click on copy button
   * @param clipEntity The Clip
   * @param view       The View
   */
  public void onCopyClick(ClipEntity clipEntity, ImageView view) {
    final Context context = view.getContext();
    Log.logD(TAG, "copy row clicked");
    Analytics.INST(context).imageClick(TAG, "clipItemCopy");
    clipEntity.setRemote(false);
    clipEntity.setDevice(MyDevice.INST(context).getDisplayName());
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
    Log.logD(TAG, "fav clicked");
    Analytics.INST(context).checkBoxClick(TAG, "clipItemFav: " + checked);
  }
}
