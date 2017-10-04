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
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

/** Activity for editing the List of {@link Label} items */
public class LabelsEditActivity extends BaseActivity {

  /** Adapter used to display the list's data */
   private LabelsEditAdapter mAdapter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_labels_edit;

    super.onCreate(savedInstanceState);

    setupRecyclerView();
  }

  @Override
  protected void onResume() {
    super.onResume();

    mAdapter.notifyDataSetChanged();
  }

  /** Connect the {@link LabelsEditAdapter} to the {@link RecyclerView} */
  private void setupRecyclerView() {
    final RecyclerView recyclerView = findViewById(R.id.labelList);
    if (recyclerView != null) {
      mAdapter = new LabelsEditAdapter(this);
      recyclerView.setAdapter(mAdapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));

      final LabelLoaderManager loaderManager = new LabelLoaderManager(mAdapter);

      // Prepare the loader. Either re-connect with an existing one,
      // or start a new one.
      getSupportLoaderManager().initLoader(0, null, loaderManager);
    }
  }
}
