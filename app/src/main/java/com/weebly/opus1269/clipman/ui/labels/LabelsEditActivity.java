/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Labels;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

public class LabelsEditActivity extends BaseActivity {

  /** Adapter used to display the list's data */
   private LabelsEditAdapter mAdapter = null;

  /** Listens for {@link Labels} changes */
  private BroadcastReceiver mReceiver = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_labels_edit;

    super.onCreate(savedInstanceState);

    mReceiver = new LabelsBroadcastReceiver();

    setupRecyclerView();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Register mReceiver to receive Labels notifications.
    LocalBroadcastManager.getInstance(this)
      .registerReceiver(mReceiver,
        new IntentFilter(Intents.FILTER_LABELS));

    mAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Unregister since the activity is not visible
    LocalBroadcastManager.getInstance(this)
      .unregisterReceiver(mReceiver);
  }

  /**
   * Connect the {@link LabelsEditAdapter} to the {@link RecyclerView}
   */
  private void setupRecyclerView() {
    final RecyclerView recyclerView =
      findViewById(R.id.labelList);
    if (recyclerView != null) {
      mAdapter = new LabelsEditAdapter();
      recyclerView.setAdapter(mAdapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
  }

  /**
   * Inner class to handle notifications of {@link Labels} changes
   */
  class LabelsBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      final Bundle bundle = intent.getBundleExtra(Intents.BUNDLE_LABELS);
      final String action = bundle.getString(Intents.ACTION_TYPE_LABELS);
      if (action == null) {
        return;
      }

      switch (action) {
        case Intents.TYPE_LABELS_UPDATED:
          mAdapter.notifyDataSetChanged();
        case Intents.TYPE_LABEL_ADDED:
          mAdapter.notifyDataSetChanged();
          break;
        case Intents.TYPE_LABEL_REMOVED:
          mAdapter.notifyDataSetChanged();
          break;
        case Intents.TYPE_LABEL_CHANGED:
          mAdapter.notifyDataSetChanged();
          break;
        default:
          break;
      }
    }
  }
}
