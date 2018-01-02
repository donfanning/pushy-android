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
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;

import java.util.ArrayList;
import java.util.List;

/** Singleton to manage the Clips.db Label and LabelMap tables */
public class LabelTables {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static LabelTables sInstance;

  /** Global Application Context */
  private final Context mContext;

  private LabelTables(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static LabelTables INST(@NonNull Context context) {
    synchronized (LabelTables.class) {
      if (sInstance == null) {
        sInstance = new LabelTables(context);
      }
      return sInstance;
    }
  }


  /**
   * Get the PK for the given {@link Label} name
   * @return PK for name, -1L if not found
   */
  public long getLabelId(@NonNull String labelName) {
    long ret = -1L;

    final ContentResolver resolver = mContext.getContentResolver();

    final String[] projection = {ClipsContract.Label._ID};
    final String selection = ClipsContract.Label.COL_NAME + " = ? ";
    final String[] selectionArgs = {labelName};

    final Cursor cursor = resolver.query(ClipsContract.Label.CONTENT_URI,
      projection, selection, selectionArgs, null);
    if ((cursor == null) || (cursor.getCount() <= 0)) {
      if (cursor != null) {
        cursor.close();
      }
      return ret;
    }

    try {
      cursor.moveToNext();
      final int idx = cursor.getColumnIndex(ClipsContract.Label._ID);
      ret = cursor.getLong(idx);
    } finally {
      cursor.close();
    }
    return ret;
  }

  /**
   * Get the List of {@link Label} objects
   * @return List of Labels
   */
  public List<Label> getLabels() {
    final ArrayList<Label> list = new ArrayList<>(0);
    final ContentResolver resolver = mContext.getContentResolver();

    final String[] projection = {" * "};

    // query for all
    final Cursor cursor = resolver.query(ClipsContract.Label.CONTENT_URI,
      projection, null, null, null);
    if ((cursor == null) || (cursor.getCount() <= 0)) {
      return list;
    }

    try {
      while (cursor.moveToNext()) {
        int idx = cursor.getColumnIndex(ClipsContract.Label.COL_NAME);
        final String name = cursor.getString(idx);
        idx = cursor.getColumnIndex(ClipsContract.Label._ID);
        final long id = cursor.getLong(idx);
        list.add(new Label(name, id));
      }
    } finally {
      cursor.close();
    }

    return list;
  }

  /**
   * Add the {@link Label} map for a group of {@link ClipItem} objects to the
   * database
   * @param clipItems the items to add Labels for
   * @return number of items added
   */
  public int insertLabelsMap(ClipItem[] clipItems) {
    if (clipItems == null) {
      return 0;
    }

    final ContentResolver resolver = mContext.getContentResolver();

    // get total number of ClipItem/Label entries
    int size = 0;
    for (ClipItem clipItem : clipItems) {
      size += clipItem.getLabels().size();
    }

    final ContentValues[] mapCVs = new ContentValues[size];
    int count = 0;
    for (ClipItem clipItem : clipItems) {
      for (Label label : clipItem.getLabels()) {
        ContentValues cv = new ContentValues();
        cv.put(ClipsContract.LabelMap.COL_CLIP_ID, clipItem.getId(mContext));
        cv.put(ClipsContract.LabelMap.COL_LABEL_NAME, label.getName());
        mapCVs[count] = cv;
        count++;
      }
    }

    return resolver.bulkInsert(ClipsContract.LabelMap.CONTENT_URI, mapCVs);
  }

  /**
   * Add a {@link ClipItem} and {@link Label} to the LabelMap table
   * @param clipItem the clip
   * @param label    the label
   * @return if true, added
   */
  public boolean insert(ClipItem clipItem, Label label) {
    if (AppUtils.isWhitespace(clipItem.getText()) ||
      AppUtils.isWhitespace(label.getName())) {
      return false;
    }

    final ContentResolver resolver = mContext.getContentResolver();

    if (exists(resolver, clipItem, label)) {
      // already in db
      return false;
    }

    // insert Label
    label.save(mContext);

    // insert into LabelMap table
    final ContentValues cv = new ContentValues();
    cv.put(ClipsContract.LabelMap.COL_CLIP_ID, clipItem.getId(mContext));
    cv.put(ClipsContract.LabelMap.COL_LABEL_NAME, label.getName());

    resolver.insert(ClipsContract.LabelMap.CONTENT_URI, cv);

    return true;
  }

  /**
   * Delete a {@link ClipItem} and {@link Label} from the LabelMap table
   * @param clipItem the clip
   * @param label    the label
   */
  public void delete(ClipItem clipItem, Label label) {
    if (ClipItem.isWhitespace(clipItem) ||
      AppUtils.isWhitespace(label.getName())) {
      return;
    }

    final ContentResolver resolver = mContext.getContentResolver();

    final long id = clipItem.getId(mContext);
    final String selection =
      ClipsContract.LabelMap.COL_LABEL_NAME + " = ? AND " +
      ClipsContract.LabelMap.COL_CLIP_ID + " = " + id;
    final String[] selectionArgs = {label.getName()};

    resolver.delete(ClipsContract.LabelMap.CONTENT_URI, selection,
      selectionArgs);
  }

  /**
   * Does the ClipItem and Label exist in the LabelMap table
   * @param resolver to db
   * @param label    Label to check
   * @return if true, already in db
   */
  private boolean exists(ContentResolver resolver, ClipItem clipItem,
                         Label label) {
    final String[] projection = {ClipsContract.LabelMap.COL_LABEL_NAME};
    final long id = clipItem.getId(mContext);
    final String selection =
      ClipsContract.LabelMap.COL_LABEL_NAME + " = ? AND " +
      ClipsContract.LabelMap.COL_CLIP_ID + " = " + id;
    final String[] selectionArgs = {label.getName()};

    final Cursor cursor = resolver.query(ClipsContract.LabelMap.CONTENT_URI,
      projection, selection, selectionArgs, null);
    if ((cursor != null) && (cursor.getCount() > 0)) {
      // found it
      cursor.close();
      return true;
    }
    return false;
  }
}
