/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.weebly.opus1269.clipman.db.entity.BackupEntity;

import java.util.List;

/** DB access for Backups */
@Dao
public interface BackupDao {
  @Query("SELECT * FROM backups ORDER BY isMine DESC, date DESC")
  LiveData<List<BackupEntity>> loadAll();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(BackupEntity... backupEntities);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<BackupEntity> backupEntities);

  @Query("DELETE FROM backups")
  void deleteAll();

  @Query("DELETE FROM backups WHERE drive_id_invariant = :driveIdInvariant")
  int delete(String driveIdInvariant);
}
