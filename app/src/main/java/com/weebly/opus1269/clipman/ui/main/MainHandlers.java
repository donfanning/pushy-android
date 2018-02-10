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

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;
import com.weebly.opus1269.clipman.viewmodel.ClipViewModel;

/** Handlers for UI events */
public class MainHandlers extends BaseHandlers {
  private final BaseActivity mActivity;
  private final String TAG;

  public MainHandlers(BaseActivity activity) {
    super();
    mActivity = activity;
    this.TAG = activity.getTAG();
  }

  /**
   * Click on fab button
   * @param view The View
   * @param clip  A Clip
   */
  public void onFabClick(View view, Clip clip) {
    final Context context = view.getContext();
    if (!Clip.isWhitespace(clip)) {
      clip.doShare(context, view);
      Analytics.INST(context).imageClick(TAG, "clipItemShare");
    } else {
      AppUtils.showMessage(context, view,
        context.getString(R.string.repo_no_clip_text));
    }
  }

  /**
   * Click on Clip item
   * @param view The View
   * @param clip The Clip
   */
  public void onItemClick(View view, Clip clip) {
    Analytics.INST(view.getContext()).click(TAG, "clipItemRow");
    ((MainActivity)mActivity).selectClip(clip);
  }

  /**
   * Click on copy button
   * @param view The View
   * @param clip The Clip
   */
  public void onCopyClick(View view, Clip clip) {
    final Context context = view.getContext();
    Analytics.INST(context).imageClick(TAG, "clipItemCopy");
    clip.setRemote(false);
    clip.copyToClipboard(context);
    if (!Prefs.INST(context).isMonitorClipboard()) {
      AppUtils.showMessage(context, mActivity.getFab(),
        context.getString(R.string.clipboard_copy));
    }
  }

  /**
   * Click on labels button
   * @param view The View
   * @param clip The Clip
   */
  public void onLabelsClick(View view, Clip clip) {
    final Context context = view.getContext();
    Log.logD(TAG, "select labels clicked");
    Analytics.INST(context).imageClick(TAG, "clipItemLabels");
    final Intent intent = new Intent(context, LabelsSelectActivity.class);
    intent.putExtra(Intents.EXTRA_CLIP, clip);
    AppUtils.startActivity(context, intent);
  }

  /**
   * Click on fav checkbox
   * @param checkBox The CheckBox
   * @param vm       The ViewModel
   */
  public void onFavClick(View checkBox, ClipViewModel vm) {
    final Context context = checkBox.getContext();
    final boolean checked = ((CheckBox)checkBox).isChecked();
    vm.changeFav(checked);
    Analytics.INST(context).checkBoxClick(TAG, "clipItemFav: " + checked);
  }
}
