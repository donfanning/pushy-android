/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.base;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

/**
 * This Fragement handles the basic stuff. Make sure you use a standard
 * naming convention for you Activities views and actions. Extend from this.
 */
public abstract class BaseFragment extends Fragment {
  /** Class identifier */
  protected final String TAG = this.getClass().getSimpleName();

  /** findViewById that checks for null Activity */
  @Nullable protected <T extends View> T findViewById(int id) {
    final Activity activity = getActivity();
    if (activity == null) {
      final View view = getView();
      if (view == null) {
        return null;
      } else {
        return view.findViewById(id);
      }
    } else {
      return activity.findViewById(id);
    }
  }
}
