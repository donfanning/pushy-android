/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;

import java.util.ArrayList;
import java.util.List;

/** Generates data to pre-populate the database */
public class MainDBInitializer {
  private static final String[] TEXT = new String[]{
    App.INST().getString(R.string.default_clip_3),
    App.INST().getString(R.string.default_clip_2),
    App.INST().getString(R.string.default_clip_1)
  };

  private static final Boolean[] FAV = new Boolean[]{true, false, true};

  public static List<ClipEntity> getClips() {
    List<ClipEntity> clipEntities = new ArrayList<>(TEXT.length);
    for (int i = 0; i < TEXT.length; i++) {
      final ClipEntity clipEntity = new ClipEntity();
      clipEntity.setText(TEXT[i]);
      clipEntity.setFav(FAV[i]);
      // so dates aren't all the same
      clipEntity.setDate(clipEntity.getDate() + i);
      clipEntities.add(clipEntity);
    }
    return clipEntities;
  }

  public static List<LabelEntity> getLabels() {
    List<LabelEntity> labelEntities = new ArrayList<>(1);
    final LabelEntity labelEntity =
      new LabelEntity(App.INST().getString(R.string.default_label_name));
    labelEntities.add(labelEntity);
    return labelEntities;
  }

}
