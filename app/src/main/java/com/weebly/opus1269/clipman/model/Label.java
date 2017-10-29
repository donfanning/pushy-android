/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.ClipsContract;

import java.io.Serializable;

/** A descriptive label for an item */
public class Label implements Serializable {

  /** The name of the label */
  @NonNull
  private String mName;

  public Label() {mName = "";}

  public Label(@NonNull String name) {mName = name;}

  public Label(Cursor cursor) {
    int idx = cursor.getColumnIndex(ClipsContract.Label.COL_NAME);
    mName = cursor.getString(idx);
  }

  @NonNull
  public String getName() {return mName;}

  public void setName(@NonNull String name) {
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    // update Label table
    final String[] selectionArgs = {mName};
    final String selection = ClipsContract.Label.COL_NAME + " = ? ";
    ContentValues cv = new ContentValues();
    cv.put(ClipsContract.Label.COL_NAME, name);
    resolver.update(ClipsContract.Label.CONTENT_URI, cv, selection,
      selectionArgs);

    // change labelFilter Pref if it is us
    final String labelFilter = Prefs.INST(context).getLabelFilter();
    if (labelFilter.equals(mName)) {
      Prefs.INST(context).setLabelFilter(name);
    }

    mName = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Label label = (Label) o;

    return mName.equals(label.mName);
  }

  @Override
  public int hashCode() {
    return mName.hashCode();
  }

  /**
   * Get the Label as a {@link ContentValues object}
   * @return Label as {@link ContentValues object}
   */
  public ContentValues getContentValues() {
    final ContentValues cv = new ContentValues();
    cv.put(ClipsContract.Label.COL_NAME, mName);
    return cv;
  }

  /**
   * Save to database
   * @return true if saved
   */
  public Boolean save() {
    if (AppUtils.isWhitespace(getName())) {
      return false;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    if (exists()) {
      // already in db
      return false;
    }

    // insert into db
    resolver.insert(ClipsContract.Label.CONTENT_URI, getContentValues());

    return true;
  }

  /**
   * Delete from database
   * @return true if deleted
   */
  public Boolean delete() {
    if (AppUtils.isWhitespace(mName)) {
      return false;
    }

    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    // delete from Label table
    final String[] selectionArgs = {mName};
    String selection = ClipsContract.Label.COL_NAME + " = ? ";
    resolver.delete(ClipsContract.Label.CONTENT_URI, selection, selectionArgs);

    // reset labelFilter Pref if we deleted it
    final String labelFilter = Prefs.INST(context).getLabelFilter();
    if (labelFilter.equals(mName)) {
      Prefs.INST(context).setLabelFilter("");
    }

    return true;
  }

  /**
   * Are we in the database
   * @return if true, label exists
   */
  private boolean exists() {
    final Context context = App.getContext();
    final ContentResolver resolver = context.getContentResolver();

    final String[] projection = {ClipsContract.Label.COL_NAME};
    final String selection = ClipsContract.Label.COL_NAME + " = ? ";
    final String[] selectionArgs = {mName};

    final Cursor cursor = resolver.query(ClipsContract.Label.CONTENT_URI,
      projection, selection, selectionArgs, null);

    if ((cursor != null) && (cursor.getCount() > 0)) {
      // found it
      cursor.close();
      return true;
    }
    return false;
  }
}
