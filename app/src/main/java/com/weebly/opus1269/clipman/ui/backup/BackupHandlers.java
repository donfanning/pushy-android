/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.backup;

import android.support.design.widget.FloatingActionButton;

import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.viewmodel.BackupsViewModel;

/** Handlers for UI events */
public class BackupHandlers {
  private final String TAG;

  BackupHandlers(String tag) {
    TAG = tag;
  }

  /**
   * Click on fab button
   * @param fab The View
   */
  public void onFabClick(BackupsViewModel vm, FloatingActionButton fab) {
    if (fab != null) {
      if (vm != null) {
        vm.refreshList();
      }
      Analytics.INST(fab.getContext()).imageClick(TAG, "refreshBackups");
    }
  }
}
