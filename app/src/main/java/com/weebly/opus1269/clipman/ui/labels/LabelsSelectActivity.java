/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.ClipContract;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

public class LabelsSelectActivity extends BaseActivity implements
  LoaderManager.LoaderCallbacks<Cursor> {

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

    // Prepare the loader. Either re-connect with an existing one,
    // or start a new one.
    getSupportLoaderManager().initLoader(0, null, this);
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

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    // Retrieve all columns
    final String[] projection = ClipContract.Label.FULL_PROJECTION;

    String selection = "(" +
      "(" + ClipContract.Label.COL_NAME + " NOTNULL) AND (" +
      ClipContract.Label.COL_NAME + " != '' ))";

    // Now create and return a CursorLoader that will take care of
    // creating a Cursor for the data being displayed.
    return new CursorLoader(
      this,
      ClipContract.Label.CONTENT_URI,
      projection,
      selection,
      null,
      null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    // Swap the new cursor in.  (The framework will take care of closing the
    // old cursor once we return.)
    mAdapter.swapCursor(cursor);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    // This is called when the last Cursor provided to onLoadFinished()
    // above is about to be closed.  We need to make sure we are no
    // longer using it.
    mAdapter.swapCursor(null);
  }

  /**
   * Connect the {@link LabelsEditAdapter} to the {@link RecyclerView}
   */
  private void setupRecyclerView() {
    final RecyclerView recyclerView =
      findViewById(R.id.labelList);
    if (recyclerView != null) {
      mAdapter = new LabelsSelectAdapter(this);
      recyclerView.setAdapter(mAdapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
  }
}
