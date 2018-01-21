/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;

import java.util.List;

/** Database access for Labels */
@Dao
public interface LabelDao {
  @Query("SELECT * FROM labels ORDER BY LOWER(name) ASC")
  LiveData<List<LabelEntity>> getAll();

  @Query("SELECT * FROM labels WHERE name = :name LIMIT 1")
  LiveData<LabelEntity> getLabel(String name);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(LabelEntity... labelEntities);

  @Query("UPDATE labels SET name = :newName WHERE name = :oldName")
  void updateName(String newName, String oldName);

  @Query("DELETE FROM labels")
  void deleteAll();

  @Delete
  void delete(LabelEntity labelEntity);
}
