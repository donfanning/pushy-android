/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.LongSparseArray;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.ClipLabelJoin;
import com.weebly.opus1269.clipman.db.entity.Label;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/** Class for moving data from old database to Room database */
@SuppressWarnings("TryFinallyCanBeTryWithResources")
class OldClipsDB implements Callable<Boolean>{
  private static final String TAG = "OldClipsDB";

  private static final String DB_NAME = "Clips.db";

  private SQLiteDatabase db;

  private List<Label> labels;

  private List<Clip> clips;

  /** Sparse Array of old clip ids and their List of label names */
  private LongSparseArray<List<String>> clipIdLabelNamesMap;

  public OldClipsDB() {
  }

  public static boolean exists() {
    File dbFile = App.INST().getDatabasePath(DB_NAME);
    return dbFile.exists();
  }

  @Override
  public Boolean call() throws SQLException {
    // load all data from old database
    loadTables();

    // add to new database
    MainDB.INST(App.INST()).labelDao().insertAll(labels);
    // get mapping between label name and id
    final List<Label> newLabels =
      MainDB.INST(App.INST()).labelDao().getAllSync();
    final Map<String, Long> labelsMap = new HashMap<>(newLabels.size());
    for (final Label label : newLabels) {
      labelsMap.put(label.getName(), label.getId());
    }

    for (Clip clip : clips) {
      final long clipId = MainDB.INST(App.INST()).clipDao().insert(clip);
      final List<String> labelNames = clipIdLabelNamesMap.get(clipId);
      if (labelNames != null) {
        for (String labelName : labelNames) {
          ClipLabelJoin join =
            new ClipLabelJoin(clipId, labelsMap.get(labelName));
          MainDB.INST(App.INST()).clipLabelJoinDao().insert(join);
        }
      }
    }
    return true;
  }

  /** Delete the old database
   * @return true if deleted or doesn't exist
   */
  public boolean delete() {
    final File dbFile = App.INST().getDatabasePath(DB_NAME);
    if (dbFile == null) {
      // already deleted
      Log.logD(TAG, "Already deleted");
      return true;
    }
    final boolean deleted = SQLiteDatabase.deleteDatabase(dbFile);
    if (deleted) {
      Log.logD(TAG, "deleted: " + dbFile.getName());
    }
    return deleted;
  }

  /** Load all the data from the old database */
  private void loadTables() {
    File dbFile = App.INST().getDatabasePath(DB_NAME);
    db = SQLiteDatabase.
      openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
    try {
      loadClips();
      loadLabels();
      loadLabelMaps();
    } finally {
      db.close();
    }
  }

  private void loadClips() {
    clips = new ArrayList<>(0);
    final Cursor cursor =
      db.query(ClipTable.NAME, null, null, null, null, null, null);
    try {
      while (cursor.moveToNext()) {
        int idx = cursor.getColumnIndex(ClipTable.ID);
        final long id = cursor.getLong(idx);
        idx = cursor.getColumnIndex(ClipTable.COL_TEXT);
        final String text = cursor.getString(idx);
        idx = cursor.getColumnIndex(ClipTable.COL_DATE);
        final long date = cursor.getLong(idx);
        idx = cursor.getColumnIndex(ClipTable.COL_FAV);
        final int favInt = cursor.getInt(idx);
        final boolean fav = (favInt == 1);
        idx = cursor.getColumnIndex(ClipTable.COL_REMOTE);
        final int remoteInt = cursor.getInt(idx);
        final boolean remote = (remoteInt == 1);
        idx = cursor.getColumnIndex(ClipTable.COL_DEVICE);
        final String device = cursor.getString(idx);
        clips.add(new Clip(id, text, date, fav, remote, device));
      }
    } finally {
      cursor.close();
    }
  }

  private void loadLabels() {
    labels = new ArrayList<>(0);
    final Cursor cursor =
      db.query(LabelTable.NAME, null, null, null, null, null, null);
    try {
      while (cursor.moveToNext()) {
        int idx = cursor.getColumnIndex(LabelTable.ID);
        final long id = cursor.getLong(idx);
        idx = cursor.getColumnIndex(LabelTable.COL_NAME);
        final String name = cursor.getString(idx);
        labels.add(new Label(name, id));
      }
    } finally {
      cursor.close();
    }
  }

  private void loadLabelMaps() {
    clipIdLabelNamesMap = new LongSparseArray<>();
    final Cursor cursor =
      db.query(LabelMapTable.NAME, null, null, null, null, null, null);
    try {
      while (cursor.moveToNext()) {
        int idx = cursor.getColumnIndex(LabelMapTable.COL_CLIP_ID);
        final long clipId = cursor.getLong(idx);
        idx = cursor.getColumnIndex(LabelMapTable.COL_LABEL_NAME);
        final String labelName = cursor.getString(idx);
        if (clipIdLabelNamesMap.indexOfKey(clipId) < 0) {
          // create List first time
          clipIdLabelNamesMap.
            put(clipId, new ArrayList<>(Collections.singletonList(labelName)));
        } else {
          // add to List
          final List<String> labelNames = clipIdLabelNamesMap.get(clipId);
          labelNames.add(labelName);
        }
      }
    } finally {
      cursor.close();
    }
  }

  /** Inner class that defines the Clip table */
  static class ClipTable {
    static final String NAME = "clip";

    static final String ID = "_id";

    static final String COL_TEXT = "text";

    static final String COL_FAV = "fav";

    static final String COL_DATE = "date";

    static final String COL_REMOTE = "remote";

    static final String COL_DEVICE = "device";
  }

  /** Inner class that defines the Label table */
  static class LabelTable {
    static final String NAME = "label";

    static final String ID = "_id";

    static final String COL_NAME = "name";
  }

  /** Inner class that defines the LabelMap table */
  static class LabelMapTable {
    static final String NAME = "label_map";

    static final String COL_CLIP_ID = "clip_id";

    static final String COL_LABEL_NAME = "label_name";
  }
}
