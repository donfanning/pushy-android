/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.base;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

/**
 * TODO: Add description.
 */
public abstract class BaseFragment extends Fragment {

  protected final String TAG = this.getClass().getSimpleName();

  @Nullable protected <T extends View> T findViewById(int id) {
    final View view = getView();
    if (view == null) {
      return null;
    } else {
      return view.findViewById(id);
    }
  }

}
