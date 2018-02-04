/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.databinding.LabelsSelectBinding;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.viewmodel.LabelsViewModel;

public class LabelsSelectActivity extends
  BaseActivity<LabelsSelectBinding> {
  /** Saved state for mClip */
  private static final String STATE_CLIP_ITEM = "clipItem";

  /** Adapter used to display the list's data */
  private LabelsSelectAdapter mAdapter;

  /** Clipitem we are modifiying the list for */
  private ClipEntity mClip;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_labels_select;
    mIsBound = true;

    super.onCreate(savedInstanceState);

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setHomeAsUpIndicator(R.drawable.ic_clear);
    }

    mClip = (ClipEntity) getIntent().getSerializableExtra(Intents.EXTRA_CLIP);

    // setup ViewModel and data binding
    LabelsViewModel vm = new LabelsViewModel(getApplication());
    LabelHandlers handlers = new LabelHandlers(this);
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(vm);
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

    // observe errors
    vm.getErrorMsg().observe(this, errorMsg -> {
      //TODO if (errorMsg != null) {
      //  mHandlers.showErrorMessage(errorMsg);
      //}
    });

    // setup RecyclerView
    mAdapter = new LabelsSelectAdapter(this);
    mBinding.content.recycler.setAdapter(mAdapter);

    // Observe labels
    vm.getLabels().observe(this, mAdapter::setList);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putSerializable(STATE_CLIP_ITEM, mClip);
  }

  @Override
  protected void onResume() {
    super.onResume();

    mAdapter.notifyDataSetChanged();
  }

  @Override
  public boolean onSupportNavigateUp() {
    // close this activity as opposed to navigating up
    finish();

    return false;
  }

  ClipEntity getClip() {return mClip;}
}
