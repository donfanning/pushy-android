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
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

public class LabelsEditActivity extends BaseActivity implements
  LoaderManager.LoaderCallbacks<Cursor> {

  /** Adapter used to display the list's data */
   private LabelsEditAdapter mAdapter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_labels_edit;

    super.onCreate(savedInstanceState);

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
      mAdapter = new LabelsEditAdapter(this);
      recyclerView.setAdapter(mAdapter);
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
  }
}
