/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.databinding.LabelCreateBinding;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.ui.base.BaseFragment;
import com.weebly.opus1269.clipman.viewmodel.LabelCreateViewModel;

/** Fragment to Create a new {@link Label} */
public class LabelCreateFragement extends BaseFragment<LabelCreateBinding> {

  public LabelCreateFragement() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    mLayoutID = R.layout.fragment_label_create;
    mIsBound = true;

    super.onCreateView(inflater, container, savedInstanceState);

    // setup ViewModel and data binding
    LabelCreateViewModel vm =
      ViewModelProviders.of(this).get(LabelCreateViewModel.class);
    final LabelCreateHandlers handlers = new LabelCreateHandlers();
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(vm);
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

    return mBinding.getRoot();
  }
//@Override
  //public View onCreateView(@NonNull LayoutInflater inflater,
  //                         ViewGroup container, Bundle savedInstanceState) {
  //  mLayoutID= R.layout.fragment_label_create;
  //  mIsBound = true;
  //  
  //  final LabelCreateBinding binding = DataBindingUtil.inflate(inflater,
  //    R.layout.fragment_label_create, container, false);
  //
  //  // setup ViewModel and data binding
  //  final LabelCreateViewModel vm = new LabelCreateViewModel(App.INST());
  //  final LabelCreateHandlers handlers = new LabelCreateHandlers();
  //  binding.setLifecycleOwner(this);
  //  binding.setVm(vm);
  //  binding.setHandlers(handlers);
  //  binding.executePendingBindings();
  //
  //  return binding.getRoot();
  //}
}

