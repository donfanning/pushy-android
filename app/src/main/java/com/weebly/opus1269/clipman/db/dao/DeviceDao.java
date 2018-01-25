/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.weebly.opus1269.clipman.db.entity.DeviceEntity;

import java.util.List;

/** DB access for Devices */
@Dao
public interface DeviceDao {
  @Query("SELECT * FROM devices ORDER BY last_seen DESC")
  LiveData<List<DeviceEntity>> loadAll();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(DeviceEntity... deviceEntities);

  @Query("DELETE FROM devices")
  void deleteAll();

  @Delete
  void delete(DeviceEntity deviceEntity);
}
