/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;

import org.joda.time.DateTime;

/**
 * A helper class to manage the Clips.db database creation and version
 * management.
 */
public class ClipsDatabaseHelper extends SQLiteOpenHelper {
  // If you change the database schema, you must increment the database version.
  private static final int DATABASE_VERSION = 2;
  private static final String DATABASE_NAME = "Clips.db";

  private static final String TEXT = " TEXT";
  private static final String INTEGER = " INTEGER";
  private static final String UNIQUE = " UNIQUE";
  private static final String NOT_NULL = " NOT NULL";

  private static final String SQL_CREATE_CLIP = "CREATE TABLE " +
    ClipsContract.Clip.TABLE_NAME + " (" +
    ClipsContract.Clip._ID + " INTEGER PRIMARY KEY" + "," +
    ClipsContract.Clip.COL_TEXT + TEXT + UNIQUE + "," +
    ClipsContract.Clip.COL_DATE + INTEGER + "," +
    ClipsContract.Clip.COL_FAV + INTEGER + "," +
    ClipsContract.Clip.COL_REMOTE + INTEGER + "," +
    ClipsContract.Clip.COL_DEVICE + TEXT +
    " );";

  private static final String SQL_CREATE_LABEL = "CREATE TABLE " +
    ClipsContract.Label.TABLE_NAME + " (" +
    ClipsContract.Label._ID + " INTEGER PRIMARY KEY" + "," +
    ClipsContract.Label.COL_NAME + TEXT + UNIQUE +
    " );";

  private static final String SQL_CREATE_LABEL_MAP = "CREATE TABLE " +
    ClipsContract.LabelMap.TABLE_NAME + " (" +
    ClipsContract.LabelMap._ID + " INTEGER PRIMARY KEY" + "," +
    ClipsContract.LabelMap.COL_CLIP_TEXT + TEXT + "," +
    ClipsContract.LabelMap.COL_LABEL_NAME + TEXT + "," +
    " FOREIGN KEY (" + ClipsContract.LabelMap.COL_LABEL_NAME + ") " +
    "REFERENCES " +
    ClipsContract.Label.TABLE_NAME + "(" + ClipsContract.Label.COL_NAME + ")" +
    " ON DELETE CASCADE" + " ON UPDATE CASCADE" + "," +
    " FOREIGN KEY (" + ClipsContract.LabelMap.COL_CLIP_TEXT + ") " +
    "REFERENCES " +
    ClipsContract.Clip.TABLE_NAME + "(" + ClipsContract.Clip.COL_TEXT + ")" +
    " ON DELETE CASCADE"  + " ON UPDATE CASCADE" +
    " );";

  private final Context mContext;

  public ClipsDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    mContext = context;
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    super.onConfigure(db);
    db.setForeignKeyConstraintsEnabled(true);
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

      createExampleLabel(db, DateTime.now().getMillis());
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
      db.replace(ClipsContract.Clip.TABLE_NAME, null, item.getContentValues());
    }

    // create some informative entries

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_5));
    item.setFav(true);
    long time = item.getTime();
    time = time + 1;
    item.setDate(time);
    db.replace(ClipsContract.Clip.TABLE_NAME, null, item.getContentValues());

    // create one with a label
    time = time + 1;
    createExampleLabel(db, time);

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_4));
    item.setFav(false);
    time = time + 1;
    item.setDate(time);
    db.replace(ClipsContract.Clip.TABLE_NAME, null, item.getContentValues());

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_3));
    item.setFav(true);
    time = time + 1;
    item.setDate(time);
    db.replace(ClipsContract.Clip.TABLE_NAME, null, item.getContentValues());

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_2));
    item.setFav(true);
    time = time + 1;
    item.setDate(time);
    db.replace(ClipsContract.Clip.TABLE_NAME, null, item.getContentValues());

    item = new ClipItem();
    item.setText(mContext.getString(R.string.default_clip_1));
    item.setFav(true);
    time = time + 1;
    item.setDate(time);
    db.replace(ClipsContract.Clip.TABLE_NAME, null, item.getContentValues());
  }

  /**
   * Create a {@link Label} and attach to a new {@link ClipItem}
   * @param db   Clips.db
   * @param time creation time
   */
  private void createExampleLabel(SQLiteDatabase db, long time) {
    // add new ClipItem
    ClipItem clipItem = new ClipItem();
    clipItem.setText(mContext.getString(R.string.default_clip_6));
    clipItem.setFav(true);
    clipItem.setDate(time);
    db.replace(ClipsContract.Clip.TABLE_NAME, null,
      clipItem.getContentValues());

    // add new Label - has to come after ClipItem here
    final Label label = new Label("Example");
    db.replace(ClipsContract.Label.TABLE_NAME, null, label.getContentValues());

    // Attach Label to ClipItem
    ContentValues cv = new ContentValues();
    cv.put(ClipsContract.LabelMap.COL_LABEL_NAME, label.getName());
    cv.put(ClipsContract.LabelMap.COL_CLIP_TEXT, clipItem.getText());
    db.replace(ClipsContract.LabelMap.TABLE_NAME, null, cv);
  }
}
