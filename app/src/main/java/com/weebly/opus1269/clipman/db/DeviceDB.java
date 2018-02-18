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

import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.dao.DeviceDao;
import com.weebly.opus1269.clipman.db.entity.Device;

/** Device database */
@Database(entities = {Device.class}, version = 1, exportSchema = false)
public abstract class DeviceDB extends RoomDatabase {
  private static final String TAG = "DeviceDB";

  private static final String DATABASE_NAME = "device.db";

  private static DeviceDB sInstance;

  private final MutableLiveData<Boolean> mIsDBCreated = new MutableLiveData<>();

  public static DeviceDB INST(final Application app) {
    if (sInstance == null) {
      synchronized (DeviceDB.class) {
        if (sInstance == null) {
          sInstance = buildDatabase(app);
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
  private static DeviceDB buildDatabase(final Application app) {
    final Context appContext = app.getApplicationContext();
    return Room.databaseBuilder(appContext, DeviceDB.class, DATABASE_NAME)
      .addCallback(new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
          super.onCreate(db);
          Log.logD(TAG, "Creating database");
          sInstance.postDatabaseCreated();
        }
      }).build();
  }

  public abstract DeviceDao deviceDao();

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
