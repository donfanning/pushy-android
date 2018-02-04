/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import com.weebly.opus1269.clipman.db.entity.DeviceEntity;

import java.util.List;

/** Database access for devices table */
@Dao
public interface DeviceDao extends BaseDao<DeviceEntity> {
  @Query("SELECT * FROM devices ORDER BY last_seen DESC")
  LiveData<List<DeviceEntity>> getAll();

  @Query("DELETE FROM devices")
  void deleteAll();
}
