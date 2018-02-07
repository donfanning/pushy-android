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

import com.weebly.opus1269.clipman.db.entity.ClipEntity;

import java.util.List;

/** Database access for clips table */
@Dao
public interface ClipDao extends BaseDao<ClipEntity> {
  @Query("SELECT * FROM clips ORDER BY date DESC")
  LiveData<List<ClipEntity>> getAll();

  @Query("SELECT * FROM clips WHERE text LIKE :query ORDER BY date DESC")
  LiveData<List<ClipEntity>> getAll(String query);

  @Query("SELECT * FROM clips ORDER BY date DESC")
  List<ClipEntity> getAllSync();

  @Query("SELECT * FROM clips ORDER BY LOWER(text) ASC")
  LiveData<List<ClipEntity>> getAllByText();

  @Query("SELECT * FROM clips WHERE text LIKE :query ORDER BY LOWER(text) ASC")
  LiveData<List<ClipEntity>> getAllByText(String query);

  @Query("SELECT * FROM clips ORDER BY fav DESC, date DESC")
  LiveData<List<ClipEntity>> getAllPinFavs();

  @Query("SELECT * FROM clips WHERE text LIKE :query ORDER BY fav DESC, date DESC")
  LiveData<List<ClipEntity>> getAllPinFavs(String query);

  @Query("SELECT * FROM clips ORDER BY fav DESC, LOWER(text) ASC")
  LiveData<List<ClipEntity>> getAllPinFavsByText();

  @Query("SELECT * FROM clips WHERE text LIKE :query ORDER BY fav DESC, LOWER(text) ASC")
  LiveData<List<ClipEntity>> getAllPinFavsByText(String query);

  @Query("SELECT * FROM clips WHERE fav = '1' ORDER BY date DESC")
  LiveData<List<ClipEntity>> getFavs();

  @Query("SELECT * FROM clips WHERE fav = '1' AND text LIKE :query ORDER BY date DESC")
  LiveData<List<ClipEntity>> getFavs(String query);

  @Query("SELECT * FROM clips WHERE fav = '0' ORDER BY date DESC")
  List<ClipEntity> getNonFavsSync();

  @Query("SELECT * FROM clips WHERE fav = '1' ORDER BY LOWER(text) ASC")
  LiveData<List<ClipEntity>> getFavsByText();

  @Query("SELECT * FROM clips WHERE fav = '1' AND text LIKE :query ORDER BY LOWER(text) ASC")
  LiveData<List<ClipEntity>> getFavsByText(String query);

  @Query("SELECT * FROM clips WHERE id = :id")
  LiveData<ClipEntity> get(long id);

  @Query("SELECT * FROM clips WHERE text = :text LIMIT 1")
  ClipEntity getSync(String text);

  @Query("UPDATE clips SET fav = :fav WHERE text = :text")
  long updateFav(String text, Boolean fav);

  @Query("DELETE FROM clips")
  int deleteAll();

  @Query("DELETE FROM clips WHERE fav = 0")
  int deleteAllNonFavs();
}
