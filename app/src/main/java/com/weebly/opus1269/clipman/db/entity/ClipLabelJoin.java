/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;

@Entity(tableName = "clips_labels_join",
  primaryKeys = { "clipId", "labelId" },
  foreignKeys = {
    @ForeignKey(entity = Clip.class,
      parentColumns = "id",
      childColumns = "clipId"),
    @ForeignKey(entity = Label.class,
      parentColumns = "id",
      childColumns = "labelId")
  })
public class ClipLabelJoin {
  public final long clipId;
  public final long labelId;

  public ClipLabelJoin(final long clipId, final long labelId) {
    this.clipId = clipId;
    this.labelId = labelId;
  }
}