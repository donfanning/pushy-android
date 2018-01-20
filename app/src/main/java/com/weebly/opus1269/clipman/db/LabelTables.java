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
import android.text.TextUtils;

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
   * Is a {@link Label} with the given name in the database
   * @param labelName the name
   * @return true if label exists
   */
  public boolean exists(String labelName) {
    if (TextUtils.isEmpty(labelName)) {
      return false;
    }

    final ContentResolver resolver = mContext.getContentResolver();

    final String[] projection = {ClipsContract.Label.COL_NAME};
    final String selection = ClipsContract.Label.COL_NAME + " = ? ";
    final String[] selectionArgs = {labelName};

    final Cursor cursor = resolver.query(ClipsContract.Label.CONTENT_URI,
      projection, selection, selectionArgs, null);

    if ((cursor != null) && (cursor.getCount() > 0)) {
      // found it
      cursor.close();
      return true;
    }
    return false;
  }

  /**
   * Get the PK for the given {@link Label} name
   * @return PK for name, -1L if not found
   */
  private long getLabelId(@NonNull String labelName) {
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
   * Get the {@link Label} objects for the given {@link ClipItem}
   * @param clipItem the clip
   * @return List of labels
   */
  public List<Label> getLabels(@NonNull ClipItem clipItem) {
    List<Label> ret = new ArrayList<>(0);
    if (ClipItem.isWhitespace(clipItem)) {
      return ret;
    }

    final ContentResolver resolver = mContext.getContentResolver();

    final String[] projection = {ClipsContract.LabelMap.COL_LABEL_NAME};
    final long id = clipItem.getId(mContext);
    final String selection = ClipsContract.LabelMap.COL_CLIP_ID + " = " + id;

    Cursor cursor = resolver.query(ClipsContract.LabelMap.CONTENT_URI,
      projection, selection, null, null);
    if (cursor == null) {
      return ret;
    }

    try {
      while (cursor.moveToNext()) {
        int idx = cursor.getColumnIndex(ClipsContract.LabelMap.COL_LABEL_NAME);
        final String name = cursor.getString(idx);
        long labelId = getLabelId(name);
        ret.add(new Label(name, labelId));
      }
    } finally {
      cursor.close();
    }

    return ret;
  }

  /**
   * Get all the {@link Label} objects
   * @return List of Labels
   */
  public List<Label> getAllLabels() {
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

  /** Delete all the {@link Label} objects from the db */
  public void deleteAllLabels() {
    final ContentResolver resolver = mContext.getContentResolver();

    resolver.delete(ClipsContract.Label.CONTENT_URI, null, null);
  }

  /**
   * Update the name of a {@link Label}
   * @param newName new name
   * @param oldName current name
   */
  public void updateLabel(@NonNull String newName,
                          @NonNull String oldName) {
    final ContentResolver resolver = mContext.getContentResolver();

    // update Label
    final String[] selectionArgs = {oldName};
    final String selection = ClipsContract.Label.COL_NAME + " = ? ";
    ContentValues cv = new ContentValues();
    cv.put(ClipsContract.Label.COL_NAME, newName);
    resolver.update(ClipsContract.Label.CONTENT_URI, cv, selection,
      selectionArgs);
  }

  /**
   * Add a {@link Label}
   * @param label Label to add
   * @return true if added
   */
  public boolean addLabel(@NonNull Label label) {
    final String name = label.getName();
    if (AppUtils.isWhitespace(name)) {
      return false;
    }

    final ContentResolver resolver = mContext.getContentResolver();

    if (exists(name)) {
      return false;
    }

    // insert into db
    resolver.insert(ClipsContract.Label.CONTENT_URI, label.getContentValues());

    return true;
  }

  /**
   * Add the {@link Label} objects
   * @param labels labels to add
   */
  public void insertLabels(@NonNull List<Label> labels) {
    final ContentResolver resolver = mContext.getContentResolver();

    final ContentValues[] cvs = new ContentValues[labels.size()];
    int count = 0;
    for (Label label : labels) {
      cvs[count] = label.getContentValues();
      count++;
    }

    resolver.bulkInsert(ClipsContract.Label.CONTENT_URI, cvs);
  }

  /**
   * Add the {@link Label} map for a group of {@link ClipItem} objects to the
   * database
   * @param clipItems The clips to add labels for
   */
  void insertLabelsMap(@NonNull List<ClipItem> clipItems) {
    if (clipItems.isEmpty()) {
      return;
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

    resolver.bulkInsert(ClipsContract.LabelMap.CONTENT_URI, mapCVs);
  }

  /**
   * Add a {@link ClipItem} and {@link Label} to the LabelMap table
   * @param clipItem the clip
   * @param label    the label
   */
  public void insert(ClipItem clipItem, Label label) {
    if (AppUtils.isWhitespace(clipItem.getText()) ||
      AppUtils.isWhitespace(label.getName())) {
      return;
    }

    final ContentResolver resolver = mContext.getContentResolver();

    if (exists(resolver, clipItem, label)) {
      // already in db
      return;
    }

    // insert Label
    label.save(mContext);

    // insert into LabelMap table
    final ContentValues cv = new ContentValues();
    cv.put(ClipsContract.LabelMap.COL_CLIP_ID, clipItem.getId(mContext));
    cv.put(ClipsContract.LabelMap.COL_LABEL_NAME, label.getName());

    resolver.insert(ClipsContract.LabelMap.CONTENT_URI, cv);
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
   * Delete a {@link Label}
   * @param label the label
   * @return true if deleted
   */
  public boolean deleteLabel(@NonNull Label label) {
    final String name = label.getName();
    if (AppUtils.isWhitespace(name)) {
      return false;
    }

    final ContentResolver resolver = mContext.getContentResolver();

    final String[] selectionArgs = {name};
    String selection = ClipsContract.Label.COL_NAME + " = ? ";

    long nRows = resolver.delete(ClipsContract.Label.CONTENT_URI, selection,
      selectionArgs);

    return (nRows != 1L);
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
