/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.weebly.opus1269.clipman.db.ClipsContract;
import com.weebly.opus1269.clipman.model.Label;

/** This class manages an adpater for {@link Label} items */
class LabelLoaderManager implements
  LoaderManager.LoaderCallbacks<Cursor> {

  /** A Context */
  private final Context mContext;

  /** Adapter used to display a list's data */
  private RecyclerViewCursorAdapter mAdapter;

  LabelLoaderManager(Context context, RecyclerViewCursorAdapter adapter) {
    mContext = context;
    mAdapter = adapter;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    // Retrieve all columns
    final String[] projection = ClipsContract.Label.FULL_PROJECTION;

    String selection = "((" +
      ClipsContract.Label.COL_NAME + " NOTNULL) AND (" +
      ClipsContract.Label.COL_NAME + " != '' ))";

    // Now create and return a CursorLoader that will take care of
    // creating a Cursor for the data being displayed.
    return new CursorLoader(
      mContext,
      ClipsContract.Label.CONTENT_URI,
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
}
