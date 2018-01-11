/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model.devices;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.CustomAsyncTask;
import com.weebly.opus1269.clipman.app.ThreadedAsyncTask;

/** Device database */
@Database(entities = {Device.class}, version = 1)
public abstract class DeviceDatabase extends RoomDatabase {
  private static DeviceDatabase INSTANCE;

  public static DeviceDatabase INST(Context context) {
    if (INSTANCE == null) {
      INSTANCE = Room
        .databaseBuilder(context.getApplicationContext(),
          DeviceDatabase.class, "Device.db")
        .build();
    }
    return INSTANCE;
  }

  public abstract DeviceDao deviceDao();

  public void add(@NonNull Device device) {
    deviceDao().insert(device);
  }

  public void remove(@NonNull Device device) {
    new deleteAsyncTask(this).execute(device);
  }

  private static class deleteAsyncTask extends ThreadedAsyncTask<Device, Void, Void> {

    private DeviceDatabase db;

    deleteAsyncTask(DeviceDatabase deviceDatabase) {
      db = deviceDatabase;
    }

    @Override
    protected Void doInBackground(final Device... params) {
      final Device device = params[0];
      db.deviceDao().delete(db.deviceDao().find(device.getModel(), device.getSn(), device.getOs()));
      return null;
    }

  }
}
