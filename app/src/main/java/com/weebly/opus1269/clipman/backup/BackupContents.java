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
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Label;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Class for the contents of a backup */
public class BackupContents {
  private List<Label> labels;
  private List<ClipItem> clipItems;

  BackupContents() {
    this.labels = new ArrayList<>(0);
    this.clipItems = new ArrayList<>(0);
  }

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
    List<ClipItem> clipItems = ClipTable.INST(context).getAll();
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

  void setLabels(@NonNull List<Label> labels) {
    this.labels = labels;
  }

  @NonNull
  List<ClipItem> getClipItems() {
    return clipItems;
  }

  void setClipItems(@NonNull List<ClipItem> clipItems) {
    this.clipItems = clipItems;
  }

  /**
   * Get the our data as a JSON string
   * @return Stringified data
   */
  String getAsJSON() {
    String ret;
    // stringify it
    final Gson gson = new Gson();
    ret = gson.toJson(this);
    return ret;
  }

  /**
   * Merge the contents of the given item with this
   * @param context - A Context
   * @param contents - contents to merge
   */
  void merge(@NonNull Context context,
                       @NonNull final BackupContents contents) {
    // Merged items
    final List<Label> outLabels = this.labels;
    final List<ClipItem> outClipItems = this.clipItems;

    // Items to be merged
    final List<Label> inLabels = contents.getLabels();
    final List<ClipItem> inClipItems = contents.getClipItems();

    // Largest label PK being used by us
    long newLabelId = getLargestId(outLabels);
    newLabelId++;

    // merge labels
    for (Label inLabel : inLabels) {
      final int pos = outLabels.indexOf(inLabel);
      if (pos == -1) {
        // new label - add one with unique id to outgoing and
        // update incoming clip references to it
        final Label addLabel = new Label(inLabel.getName(), newLabelId);
        outLabels.add(addLabel);
        newLabelId++;
        updateLabelId(inClipItems, addLabel);
      } else {
        // shared label - update incoming clip references if id's don't match
        final Label sharedLabel = outLabels.get(pos);
        if (inLabel.getId() != sharedLabel.getId()) {
          updateLabelId(inClipItems, sharedLabel);
        }
      }
    }

    // merge clips
    for (ClipItem inClipItem : inClipItems) {
      final int pos = outClipItems.indexOf(inClipItem);
      if (pos == -1) {
        // new clip - add to outgoing
        if (inClipItem.getDevice().equals(Device.getMyName(context))) {
          inClipItem.setRemote(false);
        } else {
          inClipItem.setRemote(true);
        }
        final ClipItem outClipItem = new ClipItem(context, inClipItem,
          inClipItem.getLabels(), inClipItem.getLabelsId());
        outClipItems.add(outClipItem);
      } else {
        // shared clip - merge into outgoing clip
        final ClipItem outClipItem = outClipItems.get(pos);
        if (inClipItem.isFav()) {
          // true fav has priority
          outClipItem.setFav(true);
        }
        if (inClipItem.getTime() > outClipItem.getTime()) {
          // newest has priority
          outClipItem.setDate(inClipItem.getTime());
          if (inClipItem.getDevice().equals(Device.getMyName(context))) {
            outClipItem.setRemote(false);
          } else {
            outClipItem.setRemote(true);
          }
          outClipItem.setDevice(inClipItem.getDevice());
        }
        // merge labels and labelsId
        outClipItem.addLabelsNoSave(inClipItem.getLabels());
      }
    }
  }
}
