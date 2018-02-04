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

import com.weebly.opus1269.clipman.db.entity.LabelEntity;

import java.util.List;

/** Database access for labels table */
@Dao
public interface LabelDao extends BaseDao<LabelEntity> {
  @Query("SELECT * FROM labels ORDER BY LOWER(name) ASC")
  LiveData<List<LabelEntity>> getAll();

  @Query("UPDATE OR IGNORE labels SET name = :newName WHERE name = :oldName")
  int updateName(String newName, String oldName);

  @Query("DELETE FROM labels")
  void deleteAll();
}
