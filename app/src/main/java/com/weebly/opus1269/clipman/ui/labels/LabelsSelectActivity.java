/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

public class LabelsSelectActivity extends BaseActivity  {

  /** Saved state for mClipItem */
  private static final String STATE_CLIP_ITEM = "clipItem";

  /** Adapter used to display the list's data */
  private LabelsSelectAdapter mAdapter = null;

  /** Clipitem we are modifiying the list for */
  private ClipItem mClipItem;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_labels_select;

    super.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      mClipItem =
        (ClipItem) getIntent().getSerializableExtra(Intents.EXTRA_CLIP_ITEM);
    } else {
      mClipItem =
        (ClipItem) savedInstanceState.getSerializable(STATE_CLIP_ITEM);
    }

    setupRecyclerView();
  }

  @Override
  protected void onResume() {
    super.onResume();

    mAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onPause() {
    super.onPause();

  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putSerializable(STATE_CLIP_ITEM, mClipItem);
  }

  ClipItem getClipItem() {
    return mClipItem;
  }

  /**
   * Connect the {@link LabelsEditAdapter} to the {@link RecyclerView}
   */
  private void setupRecyclerView() {
    final RecyclerView recyclerView = findViewById(R.id.labelList);
    if (recyclerView != null) {
      mAdapter = new LabelsSelectAdapter(this);
      recyclerView.setAdapter(mAdapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));

      final LabelLoaderManager loaderManager = new LabelLoaderManager(mAdapter);

      // Prepare the loader. Either re-connect with an existing one,
      // or start a new one.
      getSupportLoaderManager().initLoader(0, null, loaderManager);
    }
  }
}
