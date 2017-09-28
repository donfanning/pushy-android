/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Prefs;

import org.joda.time.DateTime;

/** Singleton to manage the Clips.db Clip table */
public enum ClipTable {
  INST;

  /**
   * Add a group of {@link ClipItem} objects to the databse
   * @param clipCVs the items to add
   * @return number of items added
   */
  public int insertClipItems(ContentValues[] clipCVs) {
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();
    return resolver.bulkInsert(ClipContract.Clip.CONTENT_URI, clipCVs);
  }

  /**
   * Get the non-favorite and optionally favorite rows in the database
   * @param includeFavs flag to indicate if favorites should be retrieved too
   * @return Array of {@link ContentValues}
   */
  public ContentValues[] getAll(Boolean includeFavs) {
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String[] projection = ClipContract.Clip.FULL_PROJECTION;

    // Select all non-favorites
    String selection = "(" + ClipContract.Clip.COL_FAV + " == 0 " + ")";
    if (includeFavs) {
      // select all favorites too
      selection += " OR (" + ClipContract.Clip.COL_FAV + " == 1 )";
    }

    final Cursor cursor = resolver.query(
      ClipContract.Clip.CONTENT_URI,
      projection,
      selection,
      null,
      null);
    if (cursor == null) {
      return new ContentValues[0];
    }

    final ContentValues[] array;
    try {
      array = new ContentValues[cursor.getCount()];
      int count = 0;
      while (cursor.moveToNext()) {
        //noinspection ObjectAllocationInLoop
        final ContentValues cv = new ContentValues();
        cv.put(ClipContract.Clip.COL_TEXT,
          cursor.getString(cursor.getColumnIndex(ClipContract.Clip.COL_TEXT)));
        cv.put(ClipContract.Clip.COL_DATE,
          cursor.getLong(cursor.getColumnIndex(ClipContract.Clip.COL_DATE)));
        cv.put(ClipContract.Clip.COL_FAV,
          cursor.getLong(cursor.getColumnIndex(ClipContract.Clip.COL_FAV)));
        cv.put(ClipContract.Clip.COL_REMOTE,
          cursor.getLong(cursor.getColumnIndex(ClipContract.Clip.COL_REMOTE)));
        cv.put(ClipContract.Clip.COL_DEVICE,
          cursor.getString(cursor.getColumnIndex(ClipContract.Clip.COL_DEVICE)));
        array[count] = cv;
        count++;
      }
    } finally {
      cursor.close();
    }

    return array;
  }

  /**
   * Delete all non-favorite and optionally favorite rows
   * @param deleteFavs flag to indicate if favorites should be deleted
   * @return Number of rows deleted
   */
  public int deleteAll(Boolean deleteFavs) {
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    // Select all non-favorites
    String selection = "(" + ClipContract.Clip.COL_FAV + " == 0 " + ")";
    if (deleteFavs) {
      // select all favorites too
      selection = selection + " OR (" + ClipContract.Clip.COL_FAV + " == 1 )";
    }

    return resolver.delete(ClipContract.Clip.CONTENT_URI, selection, null);
  }

  /**
   * Delete rows older than the storage duration
   * @return Number of rows deleted
   */
  public int deleteOldItems() {
    final Context context = App.getContext();

    final String value = Prefs.getDuration();
    if (value.equals(context.getString(R.string.ar_duration_forever))) {
      return 0;
    }

    DateTime today = DateTime.now();
    today = today.withTimeAtStartOfDay();
    DateTime deleteDate = today;
    switch (value) {
      case "day":
        deleteDate = deleteDate.minusDays(1);
        break;
      case "week":
        deleteDate = deleteDate.minusWeeks(1);
        break;
      case "month":
        deleteDate = deleteDate.minusMonths(1);
        break;
      case "year":
        deleteDate = deleteDate.minusYears(1);
        break;
      default:
        return 0;
    }

    final long deleteTime = deleteDate.getMillis();

    // Select all non-favorites older than the calculated time
    final String selection =
      "(" + ClipContract.Clip.COL_FAV + " == 0 " + ")" + " AND (" +
        ClipContract.Clip.COL_DATE + " < " + deleteTime + ")";

    final ContentResolver resolver = context.getContentResolver();
    return resolver.delete(ClipContract.Clip.CONTENT_URI, selection, null);
  }

  /**
   * Add the contents of the ClipItem to the clipboard,
   * optionally only if item text is new
   * @param clipItem  the {@link ClipItem} to insert
   * @param onNewOnly only insert if item text is not in database if true
   * @return true if inserted
   */
  public boolean insert(ClipItem clipItem, boolean onNewOnly) {
    if (AppUtils.isWhitespace(clipItem.getText())) {
      return false;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    if (onNewOnly) {
      // query for existence and skip insert if it does
      final String[] projection = {ClipContract.Clip.COL_TEXT};
      final String selection = "(" + ClipContract.Clip.COL_TEXT + " == ? )";
      final String[] selectionArgs = {clipItem.getText()};

      final Cursor cursor =
        resolver.query(ClipContract.Clip.CONTENT_URI, projection, selection,
          selectionArgs, null);
      if (cursor == null) {
        return false;
      }
      if (cursor.getCount() != 0) {
        // already in database, we are done
        cursor.close();
        return false;

      }
      cursor.close();
    }

    // insert into table
    insert(context, clipItem);

    return true;
  }

  /**
   * Add a {@link ClipItem} to the database
   * @param context the context
   * @param item    the clip to add
   */
  private void insert(Context context, ClipItem item) {
    final ContentResolver resolver = context.getContentResolver();
    resolver.insert(ClipContract.Clip.CONTENT_URI, item.getContentValues());
  }


}
