/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ErrorMsg;
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
    create(vm);
  }

  /**
   * Keyboard action
   * @param textView The view
   * @param id The IME type
   * @param keyEvent The KeyEvent
   * @param vm The ViewModel
   * @return true if handled by us
   */
  public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent,
                                LabelCreateViewModel vm) {
    if (textView.getId() == R.id.addText) {
      if (id == EditorInfo.IME_ACTION_DONE) {
        create(vm);
        return true;
      } else if (keyEvent != null) {
        final int keyAction = keyEvent.getAction();
        if (keyAction == KeyEvent.ACTION_DOWN) {
          // eat it
          return true;
        } else if (keyAction == KeyEvent.ACTION_UP) {
          create(vm);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Create a new LabelOld
   * @param vm A ViewModel
   */
  private void create(@NonNull LabelCreateViewModel vm) {
    if (!AppUtils.isWhitespace(vm.name.getValue())) {
      Log.logD(TAG, "create label");
      Analytics.INST(vm.getApplication()).imageClick(TAG, "addLabel");
      vm.create();
      vm.name.setValue("");
    } else {
      vm.setErrorMsg(new ErrorMsg("LabelOld is empty"));
    }
  }
}
