/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.ClipContract;
import com.weebly.opus1269.clipman.db.LabelTables;

import java.io.Serializable;

/** A descriptive label for an item */
public class Label implements Serializable {

  /** The name of the label */
  @NonNull
  private String mName;

  public Label() {
    mName = "";
  }

  public Label(@NonNull String name) {
    mName = name;
  }

  public Label(Cursor cursor) {
    int idx = cursor.getColumnIndex(ClipContract.Label.COL_NAME);
    mName = cursor.getString(idx);
  }

  @NonNull
  public String getName() {
    return mName;
  }

  public void setName(@NonNull String name) {
    mName = name;
  }

  /**
   * Get the Label as a {@link ContentValues object}
   * @return Label as {@link ContentValues object}
   */
  public ContentValues getContentValues() {
    final ContentValues cv = new ContentValues();
    cv.put(ClipContract.Label.COL_NAME, mName);
    return cv;
  }

  /**
   * Save to database
   * @return true if saved
   */
  public Boolean save() {
    return LabelTables.INST.insert(this);
  }
}
