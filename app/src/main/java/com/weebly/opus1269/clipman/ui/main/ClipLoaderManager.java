/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.content.Context;
import android.content.Intent;
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
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.ClipsContract;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;

/** This class manages most everything related to the main RecyclerViev */
class ClipLoaderManager implements
  LoaderManager.LoaderCallbacks<Cursor>,
  View.OnClickListener {

  /** Main app activity */
  private final MainActivity mMainActivity;

  /** Adapter being used to display the list's data */
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
    Uri uri = ClipsContract.Clip.CONTENT_URI;
    final String[] projection = ClipsContract.Clip.FULL_PROJECTION;
    final String queryString = mMainActivity.getQueryString();
    final String labelFilter = mMainActivity.getLabelFilter();

    String selection = "(" +
      "(" + ClipsContract.Clip.COL_TEXT + " NOTNULL) AND (" +
      ClipsContract.Clip.COL_TEXT + " != '' )";

    if (mMainActivity.getFavFilter()) {
      // filter by favorite setting selected
      selection += " AND (" + ClipsContract.Clip.COL_FAV + " = 1 )";
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
      // speical Uri to JOIN
      uri = ClipsContract.Clip.CONTENT_URI_JOIN;
      // filter by Label name
      selection += " AND (" + ClipsContract.LabelMap.COL_LABEL_NAME +
        " = '" + labelFilter + "' )";
    }

    selection += ")";

    // Now create and return a CursorLoader that will take care of
    // creating a Cursor for the data being displayed.
    return new CursorLoader(
      mMainActivity,
      uri,
      projection,
      selection,
      selectionArgs,
      ClipsContract.Clip.getSortOrder(mMainActivity));
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

    // Swap the new cursor in.  (The framework will take care of closing the
    // old cursor once we return.)
    mAdapter.swapCursor(cursor);

    if (cursor == null) {
      return;
    }

    if (AppUtils.isDualPane(mMainActivity)) {
      // Update the selected item and ClipViewer text.
      // Can't create a new fragment here.
      // see: http://goo.gl/IFQkPc
      if (cursor.getCount() == 0) {
        mAdapter.setSelectedItemID(-1L);
        mMainActivity.startOrUpdateClipViewer(new ClipItem(mMainActivity));
      } else {
        int pos;
        if (mAdapter.getSelectedItemID() == -1L) {
          pos = mAdapter.getSelectedPos();
        } else {
          pos = mAdapter.getPosFromItemID(mAdapter.getSelectedItemID());
        }
        pos = Math.max(0, pos);
        cursor.moveToPosition(pos);
        final int index = cursor.getColumnIndex(ClipsContract.Clip._ID);
        mAdapter.setSelectedItemID(cursor.getLong(index));
        mMainActivity
          .startOrUpdateClipViewer(new ClipItem(mMainActivity, cursor));
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
    final Context context = v.getContext();

    switch (id) {
      case R.id.clipRow:
        holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
        onItemViewClicked(holder);
        Analytics.INST(context).click(mMainActivity.getTAG(), "clipItemRow");
        break;
      case R.id.favCheckBox:
        holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
        onFavClicked(holder);
        final boolean checked = holder.favCheckBox.isChecked();
        Analytics.INST(context)
          .checkBoxClick(mMainActivity.getTAG(), "clipItemFav: " + checked);
        break;
      case R.id.copyButton:
        holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
        onCopyClicked(holder);
        Analytics.INST(context)
          .imageClick(mMainActivity.getTAG(), "clipItemCopy");
        break;
      case R.id.labelButton:
        holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
        final Intent intent =
          new Intent(mMainActivity, LabelsSelectActivity.class);
        intent.putExtra(Intents.EXTRA_CLIP_ITEM, holder.clipItem);
        AppUtils.startActivity(mMainActivity, intent);
        Analytics.INST(context)
          .imageClick(mMainActivity.getTAG(), "clipItemLabels");
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

    holder.clipItem.setFav(checked);
    holder.clipItem.save(mMainActivity);
  }

  private void onCopyClicked(ClipCursorAdapter.ClipViewHolder holder) {
    holder.clipItem.setRemote(false);
    holder.clipItem.setDevice(Device.getMyName(mMainActivity));
    holder.clipItem.copyToClipboard(mMainActivity);
    if (!Prefs.INST(mMainActivity).isMonitorClipboard()) {
      AppUtils.showMessage(mMainActivity, mMainActivity.getFab(),
        mMainActivity.getString(R.string.clipboard_copy));
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
