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
import android.net.Uri;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Prefs;

import org.joda.time.DateTime;

import java.util.List;

/** Singleton to manage the Clips.db Clip table */
public enum ClipTable {
  INST;

  /**
   * Get the ClipItem row number
   * @param clipItem clip to check
   * @return table row, -1L if not found
   */
  public long getId(ClipItem clipItem) {
    long ret = -1L;
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String[] projection = {ClipsContract.Clip._ID};
    final String selection = ClipsContract.Clip.COL_TEXT + " = ? ";
    final String[] selectionArgs = {clipItem.getText()};

    final Cursor cursor = resolver.query(ClipsContract.Clip.CONTENT_URI,
      projection, selection, selectionArgs, null);

    if ((cursor != null) && (cursor.getCount() > 0)) {
      cursor.moveToNext();
      ret = cursor.getLong(cursor.getColumnIndex(ClipsContract.Clip._ID));
      cursor.close();
      return ret;
    }
    return ret;
  }

  /**
   * Get all non-favorite and optionally favorite rows for a given {@link Label}
   * @param includeFavs flag to indicate if favorites should be retrieved too
   * @param labelFilter label to filter on
   * @return Array of {@link ClipItem} objects
   */
  public ClipItem[] getAll(Boolean includeFavs, String labelFilter) {
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    Uri uri = ClipsContract.Clip.CONTENT_URI;
    final String[] projection = ClipsContract.Clip.FULL_PROJECTION;

    // Select all non-favorites
    String selection = "((" + ClipsContract.Clip.COL_FAV + " = 0 )";
    if (includeFavs) {
      // select all favorites too
      selection += " OR (" + ClipsContract.Clip.COL_FAV + " = 1 ))";
    } else {
      selection += ")";
    }

    if (!AppUtils.isWhitespace(labelFilter)) {
      // speical Uri to JOIN
      uri = ClipsContract.Clip.CONTENT_URI_JOIN;
      // filter by Label name
      selection += " AND (" + ClipsContract.LabelMap.COL_LABEL_NAME +
        " = '" + labelFilter + "' )";
    }

    final Cursor cursor = resolver.query(
      uri,
      projection,
      selection,
      null,
      null);
    if (cursor == null) {
      return new ClipItem[0];
    }

    final ClipItem[] array;
    try {
      array = new ClipItem[cursor.getCount()];
      int count = 0;
      while (cursor.moveToNext()) {
        array[count] = new ClipItem(cursor);
        count++;
      }
    } finally {
      cursor.close();
    }

    return array;
  }

  /**
   * Add a {@link ClipItem} to the database
   * @param clipItem the clip to add
   */
  public boolean insert(ClipItem clipItem) {
    if (AppUtils.isWhitespace(clipItem.getText())) {
      return false;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    // add the ClipItem
    resolver
      .insert(ClipsContract.Clip.CONTENT_URI, clipItem.getContentValues());

    // add the label map entries
    List<Label> labels = clipItem.getLabels();
    for (Label label : labels) {
      LabelTables.INST.insert(clipItem, label);
    }

    return true;
  }

  /**
   * Update an exiting {@link ClipItem} in the database
   * @param clipItem the clip to update
   */
  public boolean update(ClipItem clipItem) {
    if (AppUtils.isWhitespace(clipItem.getText())) {
      return false;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String selection = ClipsContract.Clip.COL_TEXT + " = ? ";
    final String[] selectionArgs = {clipItem.getText()};

    // do it
    resolver.update(ClipsContract.Clip.CONTENT_URI, clipItem.getContentValues(),
        selection, selectionArgs);

    return true;
  }

  /**
   * Delete a {@link ClipItem} from the database
   * @param clipItem the clip to delete
   */
  public boolean delete(ClipItem clipItem) {
    if (AppUtils.isWhitespace(clipItem.getText())) {
      return false;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String selection = ClipsContract.Clip.COL_TEXT + " = ? ";
    final String[] selectionArgs = {clipItem.getText()};

    // delete the ClipItem
    final int nRows =
      resolver.delete(ClipsContract.Clip.CONTENT_URI, selection, selectionArgs);

    return (nRows == 1);
  }

  /**
   * Add a {@link ClipItem} optionally only if text is new
   * @param clipItem  the {@link ClipItem} to insert
   * @param onNewOnly if true, only insert if item text is new
   * @return true if inserted
   */
  public boolean insert(ClipItem clipItem, boolean onNewOnly) {
    if (AppUtils.isWhitespace(clipItem.getText())) {
      return false;
    }

    if (onNewOnly && exists(clipItem)) {
      // already exists
      return false;
    }

    // insert into table
    return insert(clipItem);
  }

  /**
   * Add a group of {@link ClipItem} objects to the databse
   * @param clipItems the items to add
   * @return number of items added
   */
  public int insertClipItems(ClipItem[] clipItems) {
    if (clipItems == null) {
      return 0;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();
    int ret;

    // add the clips
    final ContentValues[] clipCVs = new ContentValues[clipItems.length];
    for (int i = 0; i < clipItems.length; i++) {
      clipCVs[i] = clipItems[i].getContentValues();
    }
    ret = resolver.bulkInsert(ClipsContract.Clip.CONTENT_URI, clipCVs);

    // add the LabelMap
    LabelTables.INST.insertLabelsMap(clipItems);

    return ret;
  }

  /**
   * Delete all non-favorite and optionally favorite rows
   * for a given {@link Label}
   * @param deleteFavs  flag to indicate if favorites should be deleted
   * @param labelFilter label to filter on
   * @return Number of rows deleted
   */
  public int deleteAll(Boolean deleteFavs, String labelFilter) {
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();


    String selection;
    if (!AppUtils.isWhitespace(labelFilter)) {
      final String CLIP = ClipsContract.Clip.TABLE_NAME;
      final String CLIP_ID = ClipsContract.Clip._ID;
      final String CLIP_FAV = ClipsContract.Clip.COL_FAV;
      final String CLIP_DOT_ID = CLIP + "." + CLIP_ID;
      final String CLIP_DOT_FAV = CLIP + "." + CLIP_FAV;
      final String LBL = ClipsContract.LabelMap.TABLE_NAME;
      final String LBL_CLIP_ID = ClipsContract.LabelMap.COL_CLIP_ID;
      final String LBL_NAME = ClipsContract.LabelMap.COL_LABEL_NAME;
      final String LBL_DOT_CLIP_ID = LBL + "." + LBL_CLIP_ID;
      final String LBL_DOT_NAME = LBL + "." + LBL_NAME;

      String innerSelect = " SELECT " + CLIP_DOT_ID + " FROM " + CLIP +
        " INNER JOIN " + LBL + " ON " + CLIP_DOT_ID + " = " + LBL_DOT_CLIP_ID;

      String innerWhere = " WHERE " + LBL_DOT_NAME + " = '" + labelFilter + "'";
      if (!deleteFavs) {
        // select non-favs only
        innerWhere += " AND " + "( " + CLIP_DOT_FAV + " = 0 )";
      }

      selection = CLIP_ID + " IN ( " + innerSelect + innerWhere + " )";

    } else {
      // no Label filter
      if (deleteFavs) {
        // select all
        selection = null;
      } else {
        // select non-favs only
        selection = ClipsContract.Clip.COL_FAV + " = 0 ";
      }
    }

    return resolver
      .delete(ClipsContract.Clip.CONTENT_URI, selection, null);
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
      "(" + ClipsContract.Clip.COL_FAV + " = 0 " + ")" + " AND (" +
        ClipsContract.Clip.COL_DATE + " < " + deleteTime + ")";

    final ContentResolver resolver = context.getContentResolver();
    return resolver.delete(ClipsContract.Clip.CONTENT_URI, selection, null);
  }

  /**
   * Does the ClipItem exist
   * @param clipItem clip to check
   * @return if true, clip exists
   */
  private boolean exists(ClipItem clipItem) {
    final long row = getId(clipItem);
    return (row != -1L);
  }
}
