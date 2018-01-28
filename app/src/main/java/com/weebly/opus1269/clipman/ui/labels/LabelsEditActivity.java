/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.databinding.ActivityLabelsEditBinding;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.viewmodel.LabelsViewModel;

/** Activity for editing the List of {@link Label} items */
public class LabelsEditActivity extends BaseActivity {
  /** Event handlers */
  private LabelHandlers mHandlers = null;

  /** Adapter used to display the list's data */
  private LabelsEditAdapter mAdapter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_labels_edit;
    mIsBound = true;

    super.onCreate(savedInstanceState);

    // setup ViewModel and data binding
    LabelsViewModel viewModel = new LabelsViewModel(getApplication());
    mHandlers = new LabelHandlers(this);
    final ActivityLabelsEditBinding binding =
      (ActivityLabelsEditBinding) mBinding;
    binding.setLifecycleOwner(this);
    binding.setVm(viewModel);
    binding.setHandlers(mHandlers);
    binding.executePendingBindings();

    // observe errors
    viewModel.getErrorMsg().observe(this, errorMsg -> {
      //TODO if (errorMsg != null) {
      //  mHandlers.showErrorMessage(errorMsg);
      //}
    });

    // setup RecyclerView
    final RecyclerView recyclerView = findViewById(R.id.labelList);
    if (recyclerView != null) {
      setupRecyclerView(recyclerView, viewModel);
    }
  }

  /** Connect the {@link LabelsEditAdapter} to the {@link RecyclerView} */
  private void setupRecyclerView(@NonNull RecyclerView recyclerView,
                                 LabelsViewModel viewModel) {
    mAdapter = new LabelsEditAdapter(this, mHandlers);
    recyclerView.setAdapter(mAdapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    // Observe labels
    viewModel.getLabels().observe(this, labels -> mAdapter.setList(labels));
  }
}
