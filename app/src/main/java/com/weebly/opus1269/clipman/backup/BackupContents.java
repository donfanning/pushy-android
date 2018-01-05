/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.backup;


import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Immutable class for the contents of a backup */
class BackupContents {
  final private List<Label> labels;
  final private List<ClipItem> clipItems;

  private BackupContents(@NonNull List<Label> labels,
                         @NonNull List<ClipItem> clipItems) {
    this.labels = labels;
    this.clipItems = clipItems;
  }

  /**
   * Get the database data
   * @return db data
   */
  @NonNull
  static BackupContents getDB(Context context) {
    List<ClipItem> clipItems = ClipTable.INST(context).getAll(true, null);
    List<Label> labels = LabelTables.INST(context).getAllLabels();
    return new BackupContents(labels, clipItems);
  }

  /**
   * Get the database data as a JSON string
   * @return Stringified data
   */
  static String getDBAsJSON(Context context) {
    String ret;
    BackupContents contents = getDB(context);
    // stringify it
    final Gson gson = new Gson();
    ret = gson.toJson(contents);
    return ret;
  }

  /**
   * Get the contents from a raw byte array
   * @param data raw content
   * @return content of a backup
   */
  static BackupContents get(@NonNull byte[] data) {
    final JsonReader reader =
      new JsonReader(new InputStreamReader(new ByteArrayInputStream(data)));
    final Gson gson = new Gson();
    final Type type = new TypeToken<BackupContents>() {
    }.getType();
    return gson.fromJson(reader, type);
  }

  /**
   * Get the largest {@link Label} PK in the list
   * @param labels - label list
   * @return largest id, 0 if empty
   */
  static private long getLargestId(@NonNull List<Label> labels) {
    long ret = 0;
    for (Label label : labels) {
      long id = label.getId();
      ret = (id > ret) ? id : ret;
    }
    return ret;
  }

  /**
   * Update the id for the {@link Label} in all the {@link ClipItem} objects
   * @param clipItems - clipItems list
   * @param label - label to chage
   */
  static private void updateLabelId(@NonNull List<ClipItem> clipItems,
                                     @NonNull Label label) {
    for (ClipItem clipItem : clipItems) {
      clipItem.updateLabelIdNoSave(label);
    }
  }

  @NonNull
  List<Label> getLabels() {
    return labels;
  }

  @NonNull
  List<ClipItem> getClipItems() {
    return clipItems;
  }

  /**
   * Merge the contents of the given item with this and return new
   * @param context - A Context
   * @param contents - contents to merge
   * @return new object with merged content
   */
  BackupContents merge(@NonNull Context context,
                       @NonNull final BackupContents contents) {
    final List<Label> labels = new ArrayList<>(this.labels);
    final List<ClipItem> clipItems = new ArrayList<>(this.clipItems);

    final List<Label> mergeLabels = contents.getLabels();
    final List<ClipItem> mergeClipItems = contents.getClipItems();

    long newLabelId = getLargestId(labels);
    newLabelId++;

    for (Label mergeLabel : mergeLabels) {
      final int pos = labels.indexOf(mergeLabel);
      if (pos == -1) {
        // new label
        final Label addLabel = new Label(mergeLabel.getName(), newLabelId);
        labels.add(addLabel);
        newLabelId++;
        updateLabelId(mergeClipItems, addLabel);
      } else {
        // shared label
        final Label sharedLabel = labels.get(pos);
        if (mergeLabel.getId() != sharedLabel.getId()) {
          updateLabelId(mergeClipItems, sharedLabel);
        }
      }
    }

    for (ClipItem mergeClipItem : mergeClipItems) {
      final int pos = clipItems.indexOf(mergeClipItem);
      if (pos == -1) {
        // new clip - add
        final ClipItem addClipItem = new ClipItem(context, mergeClipItem,
          mergeClipItem.getLabels(), mergeClipItem.getLabelsId());
        clipItems.add(addClipItem);
      } else {
        // shared clip - merge into target clip
        final ClipItem targetClipItem = clipItems.get(pos);
        if (mergeClipItem.isFav()) {
          // true fav has priority
          targetClipItem.setFav(true);
        }
        if (mergeClipItem.getTime() > targetClipItem.getTime()) {
          // newest has priority
          targetClipItem.setDate(mergeClipItem.getTime());
          targetClipItem.setDevice(mergeClipItem.getDevice());
          targetClipItem.setRemote(true);
        }
        // merge labels and labelsId
        targetClipItem.addLabelsNoSave(mergeClipItem.getLabels());
      }
    }

    return new BackupContents(labels, clipItems);
  }
}
