/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.db.ClipsContract;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Prefs;

/**
 * This class manages most everything related to the main RecyclerView
 */

class ClipLoaderManager implements
  LoaderManager.LoaderCallbacks<Cursor>,
  View.OnClickListener {

  private final MainActivity mMainActivity;

  // Adapter being used to display the list's data
  private ClipCursorAdapter mAdapter = null;

  ClipLoaderManager(MainActivity activity) {
    mMainActivity = activity;

    setupRecyclerView();

    // Prepare the loader. Either re-connect with an existing one, or start a
    // new one.
    //noinspection ThisEscapedInObjectConstruction
    mMainActivity.getSupportLoaderManager().initLoader(0, null, this);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    Uri Uri = ClipsContract.Clip.CONTENT_URI;
    final String[] projection = ClipsContract.Clip.FULL_PROJECTION;
    final String queryString = mMainActivity.getQueryString();
    final String labelFilter = mMainActivity.getLabelFilter();

    String selection = "(" +
      "(" + ClipsContract.Clip.COL_TEXT + " NOTNULL) AND (" +
      ClipsContract.Clip.COL_TEXT + " != '' )";

    if (mMainActivity.getFavFilter()) {
      // filter by favorite setting selected
      selection += " AND (" + ClipsContract.Clip.COL_FAV + " == 1 )";
    }

    String[] selectionArgs = null;
    if (!TextUtils.isEmpty(queryString)) {
      // filter by search query
      selection += " AND (" + ClipsContract.Clip.COL_TEXT +
        " LIKE ? )";
      selectionArgs = new String[1];
      selectionArgs[0] = "%" + queryString + "%";
    }

    if (!AppUtils.isWhitespace(labelFilter)) {
      // speical Uri to fo JOIN
      Uri = ClipsContract.Clip.CONTENT_URI_JOIN;
      // filter by search query
      selection += " AND (" + ClipsContract.LabelMap.COL_LABEL_NAME +
        " == '" + labelFilter + "' )";
    }

    selection += ")";

    // Now create and return a CursorLoader that will take care of
    // creating a Cursor for the data being displayed.
    return new CursorLoader(
      mMainActivity,
      Uri,
      projection,
      selection,
      selectionArgs,
      ClipsContract.Clip.getSortOrder());
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    // Swap the new cursor in.  (The framework will take care of closing the
    // old cursor once we return.)
    mAdapter.swapCursor(data);

    if (data == null) {
      return;
    }

    if (AppUtils.isDualPane()) {
      // Update the selected item and ClipViewer text.
      // Can't create a new fragment here.
      // see: http://goo.gl/IFQkPc
      if (data.getCount() == 0) {
        mAdapter.setSelectedItemID(-1L);
        mMainActivity.startOrUpdateClipViewer(new ClipItem());
      } else {
        int pos;
        if (mAdapter.getSelectedItemID() == -1L) {
          pos = mAdapter.getSelectedPos();
        } else {
          pos = mAdapter.getPosFromItemID(mAdapter.getSelectedItemID());
        }
        pos = Math.max(0, pos);
        data.moveToPosition(pos);
        final int index = data.getColumnIndex(ClipsContract.Clip._ID);
        mAdapter.setSelectedItemID(data.getLong(index));
        mMainActivity.startOrUpdateClipViewer(new ClipItem(data));
      }
    }
  }

  @Override
  public void onLoaderReset(Loader loader) {
    // This is called when the last Cursor provided to onLoadFinished()
    // above is about to be closed.  We need to make sure we are no
    // longer using it.
    mAdapter.swapCursor(null);
  }

  @Override
  public void onClick(View v) {
    final ClipCursorAdapter.ClipViewHolder holder;
    final int id = v.getId();

    switch (id) {
      case R.id.clipRow:
        holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
        onItemViewClicked(holder);
        break;
      case R.id.favCheckBox:
        holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
        onFavClicked(holder);
        break;
      case R.id.copyButton:
        holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
        onCopyClicked(holder);
        break;
      default:
        break;
    }
  }

  ClipCursorAdapter getAdapter() {
    return mAdapter;
  }

  private void onItemViewClicked(ClipCursorAdapter.ClipViewHolder holder) {
    getAdapter().setSelectedItemID(holder.itemID);
    mMainActivity.startOrUpdateClipViewer(holder.clipItem);
  }

  private void onFavClicked(ClipCursorAdapter.ClipViewHolder holder) {
    final boolean checked = holder.favCheckBox.isChecked();
    final long fav = checked ? 1 : 0;
    final Uri uri = ContentUris.withAppendedId(ClipsContract.Clip
      .CONTENT_URI, holder.itemID);
    final ContentValues cv = new ContentValues();

    holder.clipItem.setFav(checked);

    cv.put(ClipsContract.Clip.COL_FAV, fav);
    //ClipTable.INST.insert(holder.clipItem);
    mMainActivity.getContentResolver().update(uri, cv, null, null);
  }

  private void onCopyClicked(ClipCursorAdapter.ClipViewHolder holder) {
    holder.clipItem.setRemote(false);
    holder.clipItem.setDevice(Device.getMyName());
    holder.clipItem.copyToClipboard();
    if (!Prefs.isMonitorClipboard()) {
      final Context context = App.getContext();
      AppUtils.showMessage(mMainActivity.getFab(),
        context.getString(R.string.clipboard_copy));
    }
  }

  /**
   * Initialize the main {@link RecyclerView} and connect it to its {@link
   * ClipCursorAdapter}
   */
  private void setupRecyclerView() {
    final RecyclerView recyclerView = mMainActivity.findViewById(R.id.clipList);

    // required for animations to work?
    recyclerView.setHasFixedSize(true);

    // Create an empty adapter we will use to display the loaded data.
    // We pass null for the cursor, then update it in onLoadFinished()
    // Need to pass Activity for context for all UI stuff to work
    mAdapter = new ClipCursorAdapter(mMainActivity);
    recyclerView.setAdapter(mAdapter);

    // handle touch events on the RecyclerView
    final ItemTouchHelper.Callback callback = new ClipItemTouchHelper
      (mMainActivity);
    ItemTouchHelper helper = new ItemTouchHelper(callback);
    helper.attachToRecyclerView(recyclerView);
  }
}
