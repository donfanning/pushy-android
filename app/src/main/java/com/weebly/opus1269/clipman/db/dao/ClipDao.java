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

import com.weebly.opus1269.clipman.db.entity.Clip;

import java.util.List;

/** Database access for clips table */
@Dao
public abstract class ClipDao implements BaseDao<Clip> {
  @Query("SELECT * FROM clips ORDER BY date DESC")
  public abstract LiveData<List<Clip>> getAll();

  @Query("SELECT * FROM clips WHERE text LIKE :query ORDER BY date DESC")
  public abstract LiveData<List<Clip>> getAll(String query);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId ORDER BY date DESC")
  public abstract LiveData<List<Clip>> getAll(final long labelId);

  @Query("SELECT * FROM clips ORDER BY date DESC")
  public abstract List<Clip> getAllSync();

  @Query("SELECT * FROM clips ORDER BY LOWER(text) ASC")
  public abstract LiveData<List<Clip>> getAllByText();

  @Query("SELECT * FROM clips WHERE text LIKE :query ORDER BY LOWER(text) ASC")
  public abstract LiveData<List<Clip>> getAllByText(String query);

  @Query("SELECT * FROM clips ORDER BY fav DESC, date DESC")
  public abstract LiveData<List<Clip>> getAllPinFavs();

  @Query("SELECT * FROM clips WHERE text LIKE :query ORDER BY fav DESC, date DESC")
  public abstract LiveData<List<Clip>> getAllPinFavs(String query);

  @Query("SELECT * FROM clips ORDER BY fav DESC, LOWER(text) ASC")
  public abstract LiveData<List<Clip>> getAllPinFavsByText();

  @Query("SELECT * FROM clips WHERE text LIKE :query ORDER BY fav DESC, LOWER(text) ASC")
  public abstract LiveData<List<Clip>> getAllPinFavsByText(String query);

  @Query("SELECT * FROM clips WHERE fav = '1' ORDER BY date DESC")
  public abstract LiveData<List<Clip>> getFavs();

  @Query("SELECT * FROM clips WHERE fav = '1' AND text LIKE :query ORDER BY date DESC")
  public abstract LiveData<List<Clip>> getFavs(String query);

  @Query("SELECT * FROM clips WHERE fav = '0' ORDER BY date DESC")
  public abstract List<Clip> getNonFavsSync();

  @Query("SELECT * FROM clips WHERE fav = '1' ORDER BY LOWER(text) ASC")
  public abstract LiveData<List<Clip>> getFavsByText();

  @Query("SELECT * FROM clips WHERE fav = '1' AND text LIKE :query ORDER BY LOWER(text) ASC")
  public abstract LiveData<List<Clip>> getFavsByText(String query);

  @Query("SELECT * FROM clips WHERE id = :id")
  public abstract LiveData<Clip> get(long id);

  @Query("SELECT * FROM clips WHERE text = :text LIMIT 1")
  public abstract Clip getSync(String text);

  @Query("UPDATE clips SET fav = :fav WHERE text = :text")
  public abstract long updateFav(String text, Boolean fav);

  @Query("DELETE FROM clips")
  public abstract int deleteAll();

  @Query("DELETE FROM clips WHERE fav = 0")
  public abstract int deleteAllNonFavs();
}
