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

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Prefs;

import java.util.ArrayList;
import java.util.List;

/** Singleton to manage the Clips.db Label and LabelMap tables */
public enum LabelTables {
  INST;

  /**
   * Get the List of {@link Label} objects
   * @return List of Labels
   */
  public List<Label> getLabels() {
    final ArrayList<Label> list = new ArrayList<>(0);
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String[] projection = {" * "};

    // query for all
    final Cursor cursor = resolver.query(ClipsContract.Label.CONTENT_URI,
      projection, null, null, null);
    if ((cursor == null) || (cursor.getCount() <= 0)) {
      return list;
    }

    try {
      while (cursor.moveToNext()) {
        final int idx = cursor.getColumnIndex(ClipsContract.Label.COL_NAME);
        list.add(new Label(cursor.getString(idx)));
      }
    } finally {
      cursor.close();
    }

    return list;
  }

  /**
   * Add the {@link Label} map for a group of {@link ClipItem} objects to the
   * databse
   * @param clipItems the items to add Labels for
   * @return number of items added
   */
  public int insertLabelsMap(ClipItem[] clipItems) {
    if (clipItems == null) {
      return 0;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    // get total number of ClipItem/Label entrie
    int size = 0;
    for (ClipItem clipItem : clipItems) {
      size += clipItem.getLabels().size();
    }

    final ContentValues[] mapCVs = new ContentValues[size];
    int count = 0;
    for (ClipItem clipItem : clipItems) {
      for (Label label : clipItem.getLabels()) {
        ContentValues cv = new ContentValues();
        cv.put(ClipsContract.LabelMap.COL_CLIP_ID,
          clipItem.getId());
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

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    if (exists(resolver, clipItem, label)) {
      // already in db
      return false;
    }

    // insert Label into Label table if it doesn't exist
    if (!exists(resolver, label)) {
      insert(label);
    }

    // insert into LabelMap table
    final ContentValues cv = new ContentValues();
    cv.put(ClipsContract.LabelMap.COL_CLIP_ID, clipItem.getId());
    cv.put(ClipsContract.LabelMap.COL_LABEL_NAME, label.getName());

    resolver.insert(ClipsContract.LabelMap.CONTENT_URI, cv);

    return true;
  }

  /**
   * Change a {@link Label} name in the Label and LabelMap tables
   * @param newLabel new value
   * @param oldLabel current value
   */
  public void change(Label newLabel, Label oldLabel) {
    if (AppUtils.isWhitespace(newLabel.getName()) || (oldLabel == null)) {
      return;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    // update Label table
    final String[] selectionArgs = {oldLabel.getName()};
    final String selection = ClipsContract.Label.COL_NAME + " = ? ";
    ContentValues cv = new ContentValues();
    cv.put(ClipsContract.Label.COL_NAME, newLabel.getName());
    resolver.update(ClipsContract.Label.CONTENT_URI, cv, selection,
      selectionArgs);

    // change Pref if it is us
    final String labelFilter = Prefs.getLabelFilter();
    if (labelFilter.equals(oldLabel.getName())) {
      Prefs.setLabelFilter(newLabel.getName());
    }
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

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final long id = clipItem.getId();
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
   */
  public void delete(Label label) {
    if (AppUtils.isWhitespace(label.getName())) {
      return;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    // delete from Label table
    final String[] selectionArgs = {label.getName()};
    String selection = ClipsContract.Label.COL_NAME + " = ? ";
    resolver.delete(ClipsContract.Label.CONTENT_URI, selection, selectionArgs);

    // reset prefs if we deleted it
    final String labelFilter = Prefs.getLabelFilter();
    if (labelFilter.equals(label.getName())) {
      Prefs.setLabelFilter("");
    }
  }

  /**
   * Add a {@link Label} to the database if it doesn't exist
   * @param label the label to add
   * @return true if inserted
   */
  public boolean insert(Label label) {
    if (AppUtils.isWhitespace(label.getName())) {
      return false;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    if (exists(resolver, label)) {
      // already in db
      return false;
    }

    // insert into db
    resolver.insert(ClipsContract.Label.CONTENT_URI, label.getContentValues());

    return true;
  }

  /**
   * Does the Label exist in the Label tables
   * @param resolver to db
   * @param label    Label to check
   * @return if true, Label exists
   */
  private boolean exists(ContentResolver resolver, Label label) {
    final String[] projection = {ClipsContract.Label.COL_NAME};
    final String selection = ClipsContract.Label.COL_NAME + " = ? ";
    final String[] selectionArgs = {label.getName()};

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
   * Does the ClipItem and Label exist in the LabelMap table
   * @param resolver to db
   * @param label    Label to check
   * @return if true, already in db
   */
  private boolean exists(ContentResolver resolver, ClipItem clipItem,
                         Label label) {
    final String[] projection = {ClipsContract.LabelMap.COL_LABEL_NAME};
    final long id = clipItem.getId();
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
