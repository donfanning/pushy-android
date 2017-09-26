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
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Labels;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

public class LabelsEditActivity extends BaseActivity
  implements View.OnClickListener {

  // Adapter being used to display the list's data
  private LabelsEditAdapter mAdapter = null;

  private BroadcastReceiver mReceiver = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_labels_edit;

    super.onCreate(savedInstanceState);

    // TODO remove
//    Labels.add(new Label("Item"), true);
//    Labels.add(new Label("Item 2"), true);

    setupAddLabel();

    setupRecyclerView();

    setupLabelsBroadcastReceiver();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Register mReceiver to receive Device notifications.
    LocalBroadcastManager.getInstance(this)
      .registerReceiver(mReceiver,
        new IntentFilter(Intents.FILTER_LABELS));

    // TODO need it? so relative dates get updated
    mAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Unregister since the activity is not visible
    LocalBroadcastManager.getInstance(this)
      .unregisterReceiver(mReceiver);
  }

  @Override
  public void onClick(View view) {
    if (view.getId() == R.id.addDoneButton) {
      final EditText editText = findViewById(R.id.addText);
      final String text = editText.getText().toString();
      Labels.add(new Label(text), true);
    }
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

  private void setupAddLabel() {
    int color;

    final ImageView addImage = findViewById(R.id.addImage);
    final ImageButton addDoneButton = findViewById(R.id.addDoneButton);

    // listen for click
    addDoneButton.setOnClickListener(this);

    // tint icons
    if (Prefs.isLightTheme()) {
      color = R.color.deep_teal_500;
    } else {
      color = R.color.deep_teal_200;
    }
    DrawableHelper
      .withContext(this)
      .withColor(color)
      .withDrawable(R.drawable.ic_add)
      .tint()
      .applyTo(addImage);

    if (Prefs.isLightTheme()) {
      color = android.R.color.primary_text_light;
    } else {
      color = android.R.color.primary_text_dark;
    }
    DrawableHelper
      .withContext(this)
      .withColor(color)
      .withDrawable(R.drawable.ic_done)
      .tint()
      .applyTo(addDoneButton);
  }

  /**
   * Create the {@link BroadcastReceiver} to handle changes to the list
   */
  private void setupLabelsBroadcastReceiver() {
    // handler for received Intents for the "labels" event
    mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        notifyAdapter(intent);
      }

      private void notifyAdapter(Intent intent) {
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
    };
  }

  /**
   * Refresh the list
   */
  private void doRefresh() {
    mAdapter.notifyDataSetChanged();
  }
}
