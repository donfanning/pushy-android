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
import android.support.annotation.Nullable;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;

/** Singleton to manage the Clips.db Label and LabelMap tables */
public enum LabelTables {
  INST;

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

    // query for existence and skip insert if it does
    final String[] projection = {ClipContract.LabelMap.COL_LABEL_NAME};
    final String selection = "(" +
      "(" + ClipContract.LabelMap.COL_LABEL_NAME + " == ? ) AND " +
      "(" + ClipContract.LabelMap.COL_CLIP_TEXT + " == ? )" +
      ")";
    final String[] selectionArgs = {label.getName(), clipItem.getText()};

    final Cursor cursor = resolver.query(ClipContract.LabelMap.CONTENT_URI,
      projection, selection, selectionArgs, null);
    if (cursor == null) {
      return false;
    }

    if (cursor.getCount() != 0) {
      // already in database, we are done
      cursor.close();
      return false;

    }
    cursor.close();

    // insert into table
    final ContentValues cv = new ContentValues();
    cv.put(ClipContract.LabelMap.COL_CLIP_TEXT, clipItem.getText());
    cv.put(ClipContract.LabelMap.COL_LABEL_NAME, label.getName());
    resolver.insert(ClipContract.LabelMap.CONTENT_URI, cv);

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

    final String[] selectionArgs = {oldLabel.getName()};

    // update Label table
    ContentValues cv = new ContentValues();
    cv.put(ClipContract.Label.COL_NAME, newLabel.getName());
    String selection = "(" + ClipContract.Label.COL_NAME + " == ? )";
    resolver.update(ClipContract.Label.CONTENT_URI, cv, selection,
      selectionArgs);

    // update LabelMap table
    cv = new ContentValues();
    cv.put(ClipContract.LabelMap.COL_LABEL_NAME, newLabel.getName());
    selection = "(" + ClipContract.LabelMap.COL_LABEL_NAME + " == ? )";
    resolver.update(ClipContract.LabelMap.CONTENT_URI, cv, selection,
      selectionArgs);
  }

  /**
   * Delete a {@link ClipItem} and {@link Label} from the LabelMap table
   * @param clipItem the clip
   * @param label    the label
   */
  public void delete(ClipItem clipItem, Label label) {
    if (AppUtils.isWhitespace(clipItem.getText()) ||
      AppUtils.isWhitespace(label.getName())) {
      return;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String selection = "(" +
      "(" + ClipContract.LabelMap.COL_LABEL_NAME + " == ? ) AND " +
      "(" + ClipContract.LabelMap.COL_CLIP_TEXT + " == ? )" +
      ")";
    final String[] selectionArgs = {label.getName(), clipItem.getText()};

    resolver.delete(ClipContract.LabelMap.CONTENT_URI, selection,
      selectionArgs);
  }

  /**
   * Delete a {@link Label} from the Label and LabelMap tables
   * @param label the label
   */
  public void delete(Label label) {
    if (AppUtils.isWhitespace(label.getName())) {
      return;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String[] selectionArgs = {label.getName()};

    // delete from Label table
    String selection = "(" + ClipContract.Label.COL_NAME + " == ? )";
    resolver.delete(ClipContract.Label.CONTENT_URI, selection, selectionArgs);

    // delete from LabelMap table
    selection = "(" + ClipContract.LabelMap.COL_LABEL_NAME + " == ? )";
    resolver.delete(ClipContract.LabelMap.CONTENT_URI, selection,
      selectionArgs);
  }

  /**
   * Get a cursor that contains the label names for a {@link ClipItem}
   * @param clipItem clip to check
   * @return cursor of ClipContract.LabelMap.COL_LABEL_NAME may be null
   */
  @Nullable
  public Cursor getLabelNames(ClipItem clipItem) {
    if (AppUtils.isWhitespace(clipItem.getText())) {
      return null;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String[] projection = {ClipContract.LabelMap.COL_LABEL_NAME};
    final String selection =
      "(" + ClipContract.LabelMap.COL_CLIP_TEXT + " == ? )";
    final String[] selectionArgs = {clipItem.getText()};

    return resolver.query(ClipContract.LabelMap.CONTENT_URI, projection,
      selection, selectionArgs, null);
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

    final String[] projection = {ClipContract.Label.COL_NAME};
    final String selection = "(" + ClipContract.Label.COL_NAME + " == ? )";
    final String[] selectionArgs = {label.getName()};

    // query for existence and skip insert if it does
    final Cursor cursor = resolver.query(ClipContract.Label.CONTENT_URI,
      projection, selection, selectionArgs, null);
    if (cursor == null) {
      return false;
    }
    if (cursor.getCount() > 0) {
      // already in database, we are done
      cursor.close();
      return false;
    }
    cursor.close();

    // insert into db
    resolver.insert(ClipContract.Label.CONTENT_URI, label.getContentValues());

    return true;
  }
}
