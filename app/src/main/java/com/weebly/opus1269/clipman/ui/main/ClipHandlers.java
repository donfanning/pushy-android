/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.MyDevice;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.repos.MainRepo;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;
import com.weebly.opus1269.clipman.viewmodel.ClipViewModel;
import com.weebly.opus1269.clipman.viewmodel.MainViewModel;

/** Handlers for UI events */
public class ClipHandlers extends BaseHandlers
  implements DialogInterface.OnClickListener {
  private final MainActivity mActivity;
  private final String TAG;
  private ClipEntity mClipEntity;

  ClipHandlers(MainActivity activity) {
    super();
    this.mActivity = activity;
    this.TAG = activity.getTAG();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    final Button button = ((AlertDialog) dialog).getButton(which);
    final String btnText = button.getText().toString();

    Analytics.INST(button.getContext()).buttonClick(TAG, button);

    if (mActivity.getString(R.string.button_delete).equals(btnText)) {
      Log.logD(TAG, "delete clicked");
      MainRepo.INST(App.INST()).removeClipAsync(mClipEntity);
    }
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
        //Analytics.INST(context).imageClick(TAG, "shareClipItem");
      }
  }

  /**
   * Click on copy button
   * @param clipEntity The Clip
   * @param view The View
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
   * @param view The View
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
   * @param vm     The ViewModel
   * @param checkBox The CheckBox
   */
  public void onFavClick(ClipViewModel vm, CheckBox checkBox) {
    final Context context = checkBox.getContext();
    final boolean checked = checkBox.isChecked();

    vm.changeFav(checked);
    Log.logD(TAG, "fav clicked");
    Analytics.INST(context).checkBoxClick(TAG, "clipItemFav: " + checked);
  }

  /**
   * Display a confirmation dialog
   * @param titleId  resource id of dialog title
   * @param msgId    resource id of dialog message
   * @param buttonId resource id of dialog positive button
   */
  private void showDialog(int titleId, int msgId, int buttonId) {
    final AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
      .setMessage(msgId)
      .setTitle(titleId)
      .setPositiveButton(buttonId, this)
      .setNegativeButton(R.string.button_cancel, null)
      .create();

    alertDialog.show();
  }
}
