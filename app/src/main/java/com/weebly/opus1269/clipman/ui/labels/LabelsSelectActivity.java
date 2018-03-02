/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.viewmodel.LabelsSelectViewModel;

/** Handle Label selections for a Clip */
public class LabelsSelectActivity extends BaseActivity {
  /** ViewModel */
  private LabelsSelectViewModel mVm = null;

  /** Adapter used to display the list's data */
  private LabelsSelectAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_labels_select;

    super.onCreate(savedInstanceState);

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setHomeAsUpIndicator(R.drawable.ic_clear);
    }

    // setup ViewModel
    mVm = ViewModelProviders.of(this).get(LabelsSelectViewModel.class);

    if (savedInstanceState == null) {
      Clip clip = (Clip) getIntent().getSerializableExtra(Intents.EXTRA_CLIP);
      mVm.setClip(clip);
    }

    // setup RecyclerView
    mAdapter = new LabelsSelectAdapter(this);
    RecyclerView recyclerView = findViewById(R.id.recycler);
    if (recyclerView != null) {
      recyclerView.setAdapter(mAdapter);
    }

    subscribeToViewModel();
  }

  @Override
  public boolean onSupportNavigateUp() {
    // close as opposed to navigating up
    finish();
    return false;
  }

  public LabelsSelectViewModel getVm() {
    return mVm;
  }

  /** Observe changes to ViewModel */
  private void subscribeToViewModel() {
    // Observe labels
    mVm.getLabels().observe(this, labels -> {
      if (labels != null) {
        mAdapter.submitList(labels);
      }
    });

    // Observe clip
    mVm.getClip().observe(this, clip -> {
      if (clip != null) {
        mAdapter.notifyDataSetChanged();
      }
    });
  }
}
