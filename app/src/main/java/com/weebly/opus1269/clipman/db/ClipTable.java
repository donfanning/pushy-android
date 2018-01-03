/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Prefs;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/** Singleton to manage the Clips.db Clip table */
public class ClipTable {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static ClipTable sInstance;

  /** Global Application Context */
  private final Context mContext;

  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();


  private ClipTable(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static ClipTable INST(@NonNull Context context) {
    synchronized (ClipTable.class) {
      if (sInstance == null) {
        sInstance = new ClipTable(context);
      }
      return sInstance;
    }
  }

  /**
   * Doea a {@link ClipItem} with the given text exist
   * @param clipText text to query
   * @return true if in db
   */
  public boolean exists(@NonNull String clipText) {
    boolean ret = false;
    final ContentResolver resolver = mContext.getContentResolver();

    final String[] projection = {ClipsContract.Clip._ID};
    final String selection = ClipsContract.Clip.COL_TEXT + " = ?";
    final String[] selectionArgs = {clipText};

    final Cursor cursor = resolver.query(ClipsContract.Clip.CONTENT_URI,
      projection, selection, selectionArgs, null);

    if ((cursor != null) && (cursor.getCount() > 0)) {
      ret = true;
      cursor.close();
    }
    return ret;
  }

  /**
   * Get all non-favorite and optionally favorite rows for a given {@link Label}
   * @param includeFavs flag to indicate if favorites should be retrieved too
   * @param labelFilter label to filter on
   * @return The {@link ClipItem} objects
   */
  public List<ClipItem> getAll(Boolean includeFavs, String labelFilter) {
    final ArrayList<ClipItem> ret = new ArrayList<>(100);
    final ContentResolver resolver = mContext.getContentResolver();

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
      return ret;
    }

    try {
      while (cursor.moveToNext()) {
        ret.add(new ClipItem(mContext, cursor));
      }
    } finally {
      cursor.close();
    }

    return ret;
  }

  /**
   * Add a group of {@link ClipItem} objects to the databse
   * @param clipItems the items to add
   * @return number of items added
   */
  public int insert(@NonNull List<ClipItem> clipItems) {
    if (clipItems.size() < 1) {
      return 0;
    }

    final ContentResolver resolver = mContext.getContentResolver();
    int ret;

    // add the clips
    final ContentValues[] clipCVs = new ContentValues[clipItems.size()];
    for (int i = 0; i < clipItems.size(); i++) {
      clipCVs[i] = clipItems.get(i).getContentValues();
    }
    ret = resolver.bulkInsert(ClipsContract.Clip.CONTENT_URI, clipCVs);

    // add the LabelMap
    LabelTables.INST(mContext).insertLabelsMap(clipItems);

    return ret;
  }

  /** Delete all the {@link ClipItem} objects from the db */
  public void deleteAll() {
    final ContentResolver resolver = mContext.getContentResolver();

    resolver.delete(ClipsContract.Clip.CONTENT_URI, null, null);
  }

  /**
   * Delete all non-favorite and optionally favorite rows
   * for a given {@link Label}
   * @param deleteFavs  flag to indicate if favorites should be deleted
   * @param labelFilter label to filter on
   * @return Number of rows deleted
   */
  public int deleteAll(Boolean deleteFavs, String labelFilter) {
    final ContentResolver resolver = mContext.getContentResolver();


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

  /** Delete rows older than the storage duration */
  public void deleteOldItems() {
    Log.logD(TAG, "deleteOldItems called");
    final String value = Prefs.INST(mContext).getDuration();
    if (value.equals(mContext.getString(R.string.ar_duration_forever))) {
      return;
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
        return;
    }

    final long deleteTime = deleteDate.getMillis();

    // Select all non-favorites older than the calculated time
    final String selection =
      "(" + ClipsContract.Clip.COL_FAV + " = 0 " + ")" + " AND (" +
        ClipsContract.Clip.COL_DATE + " < " + deleteTime + ")";

    final ContentResolver resolver = mContext.getContentResolver();
    resolver.delete(ClipsContract.Clip.CONTENT_URI, selection, null);
  }
}
