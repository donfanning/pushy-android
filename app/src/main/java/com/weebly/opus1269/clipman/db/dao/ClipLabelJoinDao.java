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
import com.weebly.opus1269.clipman.db.entity.ClipLabelJoin;
import com.weebly.opus1269.clipman.db.entity.Label;

import java.util.List;

/** Database access for clips_labels_join table */
@Dao
public interface ClipLabelJoinDao extends BaseDao<ClipLabelJoin> {
  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId ORDER BY date DESC")
  LiveData<List<Clip>> getClipsForLabel(final long labelId);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId AND text LIKE :query ORDER BY date DESC")
  LiveData<List<Clip>> getClipsForLabel(final long labelId, final String query);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId ORDER BY LOWER(text) ASC")
  LiveData<List<Clip>> getClipsForLabelByText(final long labelId);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId AND text LIKE :query ORDER BY LOWER(text) ASC")
  LiveData<List<Clip>> getClipsForLabelByText(final long labelId, final String query);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId ORDER BY fav DESC, date DESC")
  LiveData<List<Clip>> getClipsForLabelPinFavs(final long labelId);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId AND text LIKE :query ORDER BY fav DESC, date DESC")
  LiveData<List<Clip>> getClipsForLabelPinFavs(final long labelId, final String query);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId ORDER BY fav DESC, LOWER(text) ASC")
  LiveData<List<Clip>> getClipsForLabelPinFavsByText(final long labelId);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId AND text LIKE :query ORDER BY fav DESC, LOWER(text) ASC")
  LiveData<List<Clip>> getClipsForLabelPinFavsByText(final long labelId, final String query);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId AND fav = '1' ORDER BY date DESC")
  LiveData<List<Clip>> getClipsForLabelFavs(final long labelId);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId AND fav = '1' AND text LIKE :query ORDER BY date DESC")
  LiveData<List<Clip>> getClipsForLabelFavs(final long labelId, final String query);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId AND fav = '1' ORDER BY LOWER(text) ASC")
  LiveData<List<Clip>> getClipsForLabelFavsByText(final long labelId);

  @Query("SELECT id, text, date, fav, remote, device FROM clips INNER JOIN clips_labels_join ON clips.id=clips_labels_join.clipId WHERE clips_labels_join.labelId=:labelId AND fav = '1' AND text LIKE :query ORDER BY LOWER(text) ASC")
  LiveData<List<Clip>> getClipsForLabelFavsByText(final long labelId, final String query);

  @Query("SELECT id, name FROM labels INNER JOIN clips_labels_join ON labels.id=clips_labels_join.labelId WHERE clips_labels_join.clipId=:clipId")
  List<Label> getLabelsForClipSync(final long clipId);

  @Query("DELETE FROM clips_labels_join WHERE clipId=:clipId")
  int deleteByClipId(final long clipId);
}
