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

import com.weebly.opus1269.clipman.db.entity.ClipEntity;

import java.util.List;

/** Database access for Clips */
@Dao
public interface ClipDao {
  @Query("SELECT * FROM clips ORDER BY date DESC")
  LiveData<List<ClipEntity>> getAll();

  @Query("SELECT * FROM clips WHERE text = :text LIMIT 1")
  ClipEntity getClipSync(String text);

  @Query("SELECT * FROM clips WHERE text = :text AND fav = '1' LIMIT 1")
  ClipEntity getClipWithTrueFavSync(String text);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(ClipEntity... clipEntities);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<ClipEntity> clipEntities);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insert(ClipEntity clipEntity);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insertIfNew(ClipEntity clipEntity);

  @Query("DELETE FROM clips")
  void deleteAll();

  @Query("DELETE FROM clips WHERE fav = 0")
  void deleteAllNonFavs();

  @Delete
  void delete(ClipEntity clipEntity);
}
