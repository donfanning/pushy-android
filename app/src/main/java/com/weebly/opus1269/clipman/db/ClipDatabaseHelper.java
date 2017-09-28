/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import android.content.ClipboardManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;

/**
 * A helper class to manage the Clips.db database creation and version
 * management.
 */
public class ClipDatabaseHelper extends SQLiteOpenHelper {
  // If you change the database schema, you must increment the database version.
  private static final int DATABASE_VERSION = 2;
  private static final String DATABASE_NAME = "Clips.db";

  private static final String TEXT = " TEXT";
  private static final String INTEGER = " INTEGER";
  private static final String SQL_CREATE_CLIP = "CREATE TABLE " +
    ClipContract.Clip.TABLE_NAME + " (" +
    ClipContract.Clip._ID + " INTEGER PRIMARY KEY" + "," +
    ClipContract.Clip.COL_TEXT + TEXT + " UNIQUE " + "," +
    ClipContract.Clip.COL_DATE + INTEGER + "," +
    ClipContract.Clip.COL_FAV + INTEGER + "," +
    ClipContract.Clip.COL_REMOTE + INTEGER + "," +
    ClipContract.Clip.COL_DEVICE + TEXT +
    " );";
  private static final String SQL_CREATE_LABEL = "CREATE TABLE " +
    ClipContract.Label.TABLE_NAME + " (" +
    ClipContract.Label._ID + " INTEGER PRIMARY KEY" + "," +
    ClipContract.Label.COL_NAME + TEXT +
    " );";
  private static final String SQL_CREATE_LABEL_MAP = "CREATE TABLE " +
    ClipContract.LabelMap.TABLE_NAME + " (" +
    ClipContract.LabelMap._ID + " INTEGER PRIMARY KEY" + "," +
    ClipContract.LabelMap.COL_CLIP_TEXT + TEXT + "," +
    ClipContract.LabelMap.COL_LABEL_NAME + TEXT +
    " );";

  private final Context mContext;

  public ClipDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    mContext = context;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    // create the tables
    db.execSQL(SQL_CREATE_CLIP);
    db.execSQL(SQL_CREATE_LABEL);
    db.execSQL(SQL_CREATE_LABEL_MAP);

    initDbRows(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if ((oldVersion == 1) && (newVersion == 2)) {
      // Add the Label and LabelMap tables
      db.execSQL(SQL_CREATE_LABEL);
      db.execSQL(SQL_CREATE_LABEL_MAP);

      createExampleLabel(db);
    }
  }

  @Override
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    onUpgrade(db, oldVersion, newVersion);
  }

  /**
   * Initialize the database with some app information and what is on the
   * clipboard
   * @param db the Clips.db database
   */
  private void initDbRows(SQLiteDatabase db) {
    // create a row from the clipboard
    final ClipboardManager clipboard =
      (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
    ClipItem item = ClipItem.getFromClipboard(clipboard);
    if (item != null) {
      db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());
    }

    // create some informative entries

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_5));
    item.setFav(true);
    long time = item.getTime();
    time = time + 1;
    item.setDate(time);
    db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_4));
    item.setFav(false);
    time = time + 1;
    item.setDate(time);
    db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_3));
    item.setFav(true);
    time = time + 1;
    item.setDate(time);
    db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_2));
    item.setFav(true);
    time = time + 1;
    item.setDate(time);
    db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_1));
    item.setFav(true);
    time = time + 1;
    item.setDate(time);
    db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());

    createExampleLabel(db);
  }

  private void createExampleLabel(SQLiteDatabase db) {
    final Label label = new Label("Example");
    db.replace(ClipContract.Label.TABLE_NAME, null, label.getContentValues());
  }
}
