/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.repos.MainRepo;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.base.BaseHandlers;
import com.weebly.opus1269.clipman.viewmodel.LabelViewModel;

/** Handlers for UI events */
public class LabelHandlers extends BaseHandlers {
  private final BaseActivity mActivity;

  private final String TAG;

  /** Label that may be operated on */
  private Label mLabel;

  LabelHandlers(BaseActivity baseActivity) {
    super();
    mActivity = baseActivity;
    TAG = baseActivity.getTAG();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    final Button button = ((AlertDialog) dialog).getButton(which);
    final String btnText = button.getText().toString();

    if (mActivity.getString(R.string.button_delete).equals(btnText)) {
      Analytics.INST(button.getContext()).buttonClick(TAG, "labelDelete");
      Log.logD(TAG, "delete clicked");
      MainRepo.INST(App.INST()).removeLabel(mLabel);
    }
  }

  /**
   * Click on delete button
   * @param context     A context
   * @param label The Label
   */
  public void onDeleteClick(Context context, Label label) {
    Analytics.INST(context).imageClick(TAG, "deleteLabel");
    mLabel = label;
    showConfirmationDialog(context, R.string.label_delete_dialog_title,
      R.string.label_delete_dialog_message, R.string.button_delete);
  }

  /**
   * Listen for FocusChange events on the Label view
   * @param vm The ViewModel
   * @return The listener
   */
  public View.OnFocusChangeListener OnFocusChangeListener(LabelViewModel vm) {
    return (view, isFocused) -> {
      if (!isFocused) {
        vm.updateLabel();
      }
    };
  }
}
