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

import com.weebly.opus1269.clipman.db.dao.BackupDao;
import com.weebly.opus1269.clipman.db.entity.BackupEntity;

/** Device database */
@Database(entities = {BackupEntity.class}, version = 1, exportSchema = false)
public abstract class BackupDB extends RoomDatabase {
  private static BackupDB sInstance;

  private static final String DATABASE_NAME = "backup.db";

  public abstract BackupDao backupDao();

  private final MutableLiveData<Boolean> mIsDBCreated = new MutableLiveData<>();

  public static BackupDB INST(final Application app) {
    if (sInstance == null) {
      synchronized (BackupDB.class) {
        if (sInstance == null) {
          sInstance =
            buildDatabase(app);
          sInstance.updateDatabaseCreated(app.getApplicationContext());
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
  private static BackupDB buildDatabase(final Application app) {
    final Context appContext = app.getApplicationContext();
    return Room.databaseBuilder(appContext, BackupDB.class, DATABASE_NAME)
      .addCallback(new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
          super.onCreate(db);
          BackupDB.INST(app).setDatabaseCreated();
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
}
