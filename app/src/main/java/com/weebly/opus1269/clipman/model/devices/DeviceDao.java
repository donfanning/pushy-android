/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model.devices;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

/**
 * Dao for a {@link Device}
 */
@Dao
public interface DeviceDao {
  @Query("SELECT * FROM devices ORDER BY last_seen DESC")
  LiveData<List<Device>> getAll();

  @Query("SELECT * FROM devices WHERE model = :model AND "
    + "sn = :sn AND os = :os LIMIT 1")
  Device find(String model, String sn, String os);

  @Insert(onConflict = REPLACE)
  void insert(Device device);

  @Insert
  void insertAll(Device... devices);

  @Delete
  void delete(Device device);
}
