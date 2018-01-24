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
import com.weebly.opus1269.clipman.app.AppExecutors;
import com.weebly.opus1269.clipman.db.dao.ClipDao;
import com.weebly.opus1269.clipman.db.dao.LabelDao;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;

import java.util.List;

/** Main database */
@Database(entities = {ClipEntity.class, LabelEntity.class}, version = 1, exportSchema = false)
public abstract class MainDB extends RoomDatabase {
  private static MainDB sInstance;

  private static final String DATABASE_NAME = "main.db";

  public abstract ClipDao clipDao();

  public abstract LabelDao labelDao();

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
    final Context appContext = app.getApplicationContext();
    final AppExecutors executors = App.getExecutors();
    return Room.databaseBuilder(appContext, MainDB.class, DATABASE_NAME)
      .addCallback(new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
          super.onCreate(db);
          executors.diskIO().execute(() -> {
            // Generate the data for pre-population
            MainDB database = MainDB.INST(app);
            // TODO convert Clips.db database here
            List<ClipEntity> clips = MainDBInitializer.getClips();
            List<LabelEntity> labels = MainDBInitializer.getLabels();

            insertData(database, clips, labels);
             //notify that the database was created and it's ready to be used
            database.setDatabaseCreated();
          });
        }
      }).build();
  }

  /**
   * Check whether the database already exists and expose it via
   * {@link #getDatabaseCreated()}
   */
  private void updateDatabaseCreated(final Context context) {
    if (context.getDatabasePath(DATABASE_NAME).exists()) {
      setDatabaseCreated();
    }
  }

  public LiveData<Boolean> getDatabaseCreated() {
    return mIsDBCreated;
  }

  private void setDatabaseCreated(){
    mIsDBCreated.postValue(true);
  }

  private static void insertData(final MainDB database, final List<ClipEntity> clips, final List<LabelEntity> labels) {
    database.runInTransaction(() -> {
      database.clipDao().insertAll(clips);
      database.labelDao().insertAll(labels);
    });
  }
}
