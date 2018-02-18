/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.dao.ClipDao;
import com.weebly.opus1269.clipman.db.dao.ClipLabelJoinDao;
import com.weebly.opus1269.clipman.db.dao.LabelDao;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.ClipLabelJoin;
import com.weebly.opus1269.clipman.db.entity.Label;

import java.util.List;

/** Main database */
@Database(entities = {Clip.class, Label.class, ClipLabelJoin.class},
  version = 1, exportSchema = false)
public abstract class MainDB extends RoomDatabase {
  private static final String TAG = "MainDB";

  private static final String DATABASE_NAME = "main.db";

  private static MainDB sInstance;

  private final MutableLiveData<Boolean> mIsDBCreated = new MutableLiveData<>();

  public static MainDB INST(final Application app) {
    if (sInstance == null) {
      synchronized (MainDB.class) {
        if (sInstance == null) {
          sInstance = buildDatabase(app);
          sInstance.updateDatabaseCreated(app);
        }
      }
    }
    return sInstance;
  }

  /**
   * Build the database. {@link Builder#build()} only sets up the database
   * configuration and creates a new instance of the database.
   * The SQLite database is only created when it's accessed for the first time.
   */
  private static MainDB buildDatabase(final Application app) {
    return Room.databaseBuilder(app, MainDB.class, DATABASE_NAME)
      .addCallback(new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
          super.onCreate(db);
          Log.logD(TAG, "Creating database");
          sInstance.initializeDB();
        }
      }).build();
  }

  public abstract ClipDao clipDao();

  public abstract LabelDao labelDao();

  public abstract ClipLabelJoinDao clipLabelJoinDao();

  private void initializeDB() {
    if (OldClipsDB.exists()) {
      // populate with old database
      Log.logD(TAG, "converting Clips.db");
      final OldClipsDB oldClipsDB = new OldClipsDB();
      oldClipsDB.loadTables();
      oldClipsDB.addToMainDB();
      postDatabaseCreated();
    } else {
      // populate with introductory items
      Log.logD(TAG, "adding introductory items");
      final Clip labeledClip = MainDBInitializer.getLabeledClip();
      final Label label = MainDBInitializer.getLabel();
      final List<Clip> clips = MainDBInitializer.getClips();
      App.getExecutors().diskIO().execute(() -> {
        sInstance.runInTransaction(() -> {
          sInstance.clipDao().insertAll(clips);
          final long clipId = sInstance.clipDao().insert(labeledClip);
          final long labelId = sInstance.labelDao().insert(label);
          sInstance.clipLabelJoinDao().insert(new ClipLabelJoin(clipId,
            labelId));
          postDatabaseCreated();
        });
      });
    }
  }

  /**
   * Check whether the database already exists and expose it via
   * {@link #getDatabaseCreated()}
   */
  private void updateDatabaseCreated(final Context context) {
    if (context.getDatabasePath(DATABASE_NAME).exists()) {
      postDatabaseCreated();
    }
  }

  public LiveData<Boolean> getDatabaseCreated() {
    return mIsDBCreated;
  }

  private void postDatabaseCreated() {
    mIsDBCreated.postValue(true);
  }
}
