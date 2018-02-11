/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.databinding.LabelsEditBinding;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.viewmodel.LabelsViewModel;

/** Activity for editing the List of {@link Label} items */
public class LabelsEditActivity extends BaseActivity<LabelsEditBinding> {
  /** ViewModel */
  private LabelsViewModel mVm = null;

  /** Adapter used to display the list's data */
  private LabelsEditAdapter mAdapter;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_labels_edit;
    mIsBound = true;

    super.onCreate(savedInstanceState);

    // setup ViewModel and data binding
    mVm = ViewModelProviders.of(this).get(LabelsViewModel.class);
    final LabelHandlers handlers = new LabelHandlers(this);
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(mVm);
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

    // setup RecyclerView
    mAdapter = new LabelsEditAdapter(this, handlers);
    mBinding.content.recycler.setAdapter(mAdapter);

    subscribeToViewModel();
  }

  /** Observe changes to ViewModel */
  private void subscribeToViewModel() {
    // observe error
    mVm.getErrorMsg().observe(this, errorMsg -> {
      if (errorMsg != null) {
        AppUtils.showMessage(this, mBinding.getRoot(), errorMsg.msg);
      }
    });

    // Observe labels
    mVm.getLabels().observe(this, mAdapter::setList);
  }
}
