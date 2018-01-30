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
  LiveData<List<ClipEntity>> loadAll();

  @Query("SELECT * FROM clips ORDER BY LOWER(text) ASC")
  LiveData<List<ClipEntity>> loadAllByText();

  @Query("SELECT * FROM clips ORDER BY fav DESC, date DESC")
  LiveData<List<ClipEntity>> loadAllPinFavs();

  @Query("SELECT * FROM clips ORDER BY fav DESC, LOWER(text) ASC")
  LiveData<List<ClipEntity>> loadAllPinFavsByText();

  @Query("SELECT * FROM clips WHERE fav = '1' ORDER BY date DESC")
  LiveData<List<ClipEntity>> loadFavs();

  @Query("SELECT * FROM clips WHERE fav = '1' ORDER BY LOWER(text) ASC")
  LiveData<List<ClipEntity>> loadFavsByText();

  @Query("SELECT * FROM clips WHERE text = :text LIMIT 1")
  LiveData<ClipEntity> load(String text);

  @Query("SELECT * FROM clips WHERE text = :text LIMIT 1")
  ClipEntity get(String text);

  @Query("SELECT * FROM clips WHERE text = :text AND fav = '1' LIMIT 1")
  ClipEntity getIfTrueFav(String text);

  @Query("UPDATE clips SET text = :newText WHERE text = :oldText")
  long updateText(String newText, String oldText);

  @Query("UPDATE clips SET fav = :fav WHERE text = :text")
  long updateFav(String text, Boolean fav);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<ClipEntity> clipEntities);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insert(ClipEntity clipEntity);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insertIfNew(ClipEntity clipEntity);

  @Query("DELETE FROM clips")
  int deleteAll();

  @Query("DELETE FROM clips WHERE fav = 0")
  int deleteAllNonFavs();

  @Delete
  int delete(ClipEntity clipEntity);
}
