/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.ClipsContract;
import com.weebly.opus1269.clipman.db.LabelTables;

import java.io.Serializable;

/** A descriptive label for an item */
public class Label implements Serializable {

  /** The name of the label */
  @NonNull
  private String name;

  /** PK of the label - only used for backup/restore */
  final private long _id;

  @SuppressWarnings("unused")
  public Label() {
    name = "";
    _id = -1L;
  }

  public Label(@NonNull String name, long id) {
    this.name = name;
    _id = id;
  }

  public Label(@NonNull String name) {
    this.name = name;
    _id = -1L;
  }

  public Label(Cursor cursor) {
    int idx = cursor.getColumnIndex(ClipsContract.Label.COL_NAME);
    name = cursor.getString(idx);
    idx = cursor.getColumnIndex(ClipsContract.Label._ID);
    _id = cursor.getLong(idx);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Label label = (Label) o;

    return name.equals(label.name);
  }

  /**
   * Get name
   * @return name
   */
  @NonNull
  public String getName() {return name;}

  /**
   * Get id
   * @return name
   */
  public long getId() {return _id;}

  /**
   * Change name and update database
   * @param context A Context
   * @param name    The new name
   */
  public void setName(Context context, @NonNull String name) {
    // update db
    LabelTables.INST(context).updateLabel(name, this.name);

    // change labelFilter Pref if it is us
    final String labelFilter = Prefs.INST(context).getLabelFilter();
    if (labelFilter.equals(this.name)) {
      Prefs.INST(context).setLabelFilter(name);
    }

    this.name = name;
  }

  /**
   * Get the Label as a {@link ContentValues object}
   * @return Label as {@link ContentValues object}
   */
  public ContentValues getContentValues() {
    final ContentValues cv = new ContentValues();
    cv.put(ClipsContract.Label.COL_NAME, name);
    return cv;
  }

  /**
   * Save to database
   * @return true if saved
   */
  public Boolean save(Context context) {
    return LabelTables.INST(context).addLabel(this);
  }

  /**
   * Delete from database
   * @return true if deleted
   */
  public Boolean delete(Context context) {
    final boolean deleted = LabelTables.INST(context).deleteLabel(this);

    if (deleted) {
      // reset labelFilter Pref if we deleted it
      final String labelFilter = Prefs.INST(context).getLabelFilter();
      if (labelFilter.equals(name)) {
        Prefs.INST(context).setLabelFilter("");
      }
    }

    return deleted;
  }
}
