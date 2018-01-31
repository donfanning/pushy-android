/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.base;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * This Fragement handles the basic stuff. Make sure you use a standard
 * naming convention for views and actions. Extend from this.
 */
public abstract class BaseFragment<U extends ViewDataBinding> extends Fragment {
  /** Class identifier */
  protected final String TAG = this.getClass().getSimpleName();

  /** Content view resource id */
  protected int mLayoutID = -1;

  /** True is using data binding */
  protected boolean mIsBound = false;

  /** Our data bining */
  protected U mBinding = null;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    final View rootView;
    if (mIsBound) {
      mBinding = DataBindingUtil
        .inflate(inflater, mLayoutID, container, false);
      rootView = mBinding.getRoot();
    } else {
      rootView = inflater.inflate(mLayoutID, container, false);
    }

    return rootView;
  }

  /**
   * findViewById that checks for null Activity
   * @param id Resource id
   * @param <T> Typed View
   * @return A view, null if not found
   */
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
