/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.backup;


import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;

import java.util.List;

/** Immutable class for the contents of a backup */
public class BackupContents {
  final private List<Label> labels;
  final private List<ClipItem> clipItems;

  BackupContents(@NonNull List<Label> labels,
                 @NonNull List<ClipItem> clipItems) {
    this.labels = labels;
    this.clipItems = clipItems;
  }

  @NonNull
  public List<Label> getLabels() {
    return labels;
  }

  @NonNull
  List<ClipItem> getClipItems() {
    return clipItems;
  }

  /**
   * Merge the contents of the given item into this
   * @param contents - contents to merge
   * @return new object with merged content
   */
  BackupContents merge(@NonNull final BackupContents contents) {
    return null;
  }
}
