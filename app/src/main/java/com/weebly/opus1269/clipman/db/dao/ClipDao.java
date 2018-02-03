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
import android.arch.persistence.room.Update;

import com.weebly.opus1269.clipman.db.entity.ClipEntity;

import java.util.List;

/** Database access for Clips */
@Dao
public interface ClipDao {
  @Query("SELECT * FROM clips ORDER BY date DESC")
  LiveData<List<ClipEntity>> getAll();

  @Query("SELECT * FROM clips ORDER BY LOWER(text) ASC")
  LiveData<List<ClipEntity>> getAllByText();

  @Query("SELECT * FROM clips ORDER BY fav DESC, date DESC")
  LiveData<List<ClipEntity>> getAllPinFavs();

  @Query("SELECT * FROM clips ORDER BY fav DESC, LOWER(text) ASC")
  LiveData<List<ClipEntity>> getAllPinFavsByText();

  @Query("SELECT * FROM clips WHERE fav = '1' ORDER BY date DESC")
  LiveData<List<ClipEntity>> getFavs();

  @Query("SELECT * FROM clips WHERE fav = '1' ORDER BY LOWER(text) ASC")
  LiveData<List<ClipEntity>> getFavsByText();

  @Query("SELECT * FROM clips WHERE id = :id")
  LiveData<ClipEntity> get(long id);

  @Query("SELECT * FROM clips WHERE text = :text LIMIT 1")
  ClipEntity getSync(String text);

  @Query("SELECT * FROM clips WHERE text = :text AND fav = '1' LIMIT 1")
  ClipEntity getIfTrueFavSync(String text);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<ClipEntity> clipEntities);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insert(ClipEntity clipEntity);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insertIfNew(ClipEntity clipEntity);

  @Update(onConflict = OnConflictStrategy.IGNORE)
  int update(ClipEntity clipEntity);

  @Query("UPDATE clips SET fav = :fav WHERE text = :text")
  long updateFav(String text, Boolean fav);

  @Query("DELETE FROM clips")
  int deleteAll();

  @Query("DELETE FROM clips WHERE fav = 0")
  int deleteAllNonFavs();

  @Delete
  int delete(ClipEntity clipEntity);
}
