/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Update;

import java.util.List;

/** Common insert, update, delete methods */
@Dao
public interface BaseDao<T> {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insert(T obj);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insertIfNew(T obj);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long[] insertAll(List<T> obj);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long[] insertAllIfNew(List<T> obj);

  @Update(onConflict = OnConflictStrategy.IGNORE)
  int update(T obj);

  @Update(onConflict = OnConflictStrategy.IGNORE)
  int updateAll(List<T> obj);

  @Delete
  int delete(T obj);

  @Delete
  int deleteAll(List<T> obj);
}
