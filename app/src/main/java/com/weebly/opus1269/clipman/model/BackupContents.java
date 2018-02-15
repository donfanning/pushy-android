/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.db.MainDB;
import com.weebly.opus1269.clipman.db.entity.ClipItem;
import com.weebly.opus1269.clipman.db.entity.ClipLabelJoin;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Class for the contents of a backup */
public class BackupContents {
  // Important: Don't changes these names
  @Expose
  private List<Label> labels;

  @Expose
  private List<ClipItem> clipItems;

  public BackupContents() {
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
   * @return database contents
   */
  @NonNull
  public static BackupContents getDB() {
    List<ClipItem> clips = MainRepo.INST(App.INST()).getClipsSync();
    List<Label> labels = MainRepo.INST(App.INST()).getLabelsSync();
    return new BackupContents(labels, clips);
  }

  /**
   * Get the database data as a JSON string
   * @return Stringified data
   */
  public static String getDBAsJSON() {
    String ret;
    BackupContents contents = getDB();
    // stringify it
    final Gson gson = new GsonBuilder()
      .excludeFieldsWithoutExposeAnnotation()
      .create();
    ret = gson.toJson(contents);
    return ret;
  }

  /**
   * Get the contents from a raw byte array
   * @param data raw content
   * @return content of a backup
   */
  public static BackupContents get(@NonNull byte[] data) {
    final JsonReader reader =
      new JsonReader(new InputStreamReader(new ByteArrayInputStream(data)));
    final Gson gson = new Gson();
    final Type type = new TypeToken<BackupContents>() {}.getType();
    return gson.fromJson(reader, type);
  }

  /**
   * Get the largest {@link Label} PK in the list
   * @param labels label list
   * @return largest id, 0 if empty
   */
  private static long getLargestId(@NonNull List<Label> labels) {
    long ret = 0;
    for (Label label : labels) {
      long id = label.get_Id();
      ret = (id > ret) ? id : ret;
    }
    return ret;
  }

  /**
   * Update the id for the {@link Label} in all the {@link ClipItem} objects
   * @param clips clipItems list
   * @param label label to chage
   */
  private static void updateLabelId(@NonNull List<ClipItem> clips,
                                    @NonNull Label label) {
    for (ClipItem clip : clips) {
      clip.updateLabelId(label);
    }
  }

  @NonNull
  public List<Label> getLabels() {
    return labels;
  }

  public void setLabels(@NonNull List<Label> labels) {
    this.labels = labels;
  }

  @NonNull
  public List<ClipItem> getClipItems() {
    return clipItems;
  }

  public void setClipItems(@NonNull List<ClipItem> clipItems) {
    this.clipItems = clipItems;
  }

  /**
   * Get our contents as a JSON string
   * @return Stringified contents
   */
  public String getAsJSON() {
    String ret;
    // stringify it
    final Gson gson = new GsonBuilder()
      .excludeFieldsWithoutExposeAnnotation()
      .create();
    ret = gson.toJson(this);
    return ret;
  }

  /**
   * Merge the contents of the given item with this
   * @param context  A Context
   * @param contents contents to merge
   */
  public void merge(@NonNull Context context,
                    @NonNull final BackupContents contents) {
    // Merged items
    final List<Label> outLabels = this.labels;
    final List<ClipItem> outClips = this.clipItems;

    // Items to be merged
    final List<Label> inLabels = contents.getLabels();
    final List<ClipItem> inClips = contents.getClipItems();

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
        updateLabelId(inClips, addLabel);
      } else {
        // shared label - update incoming clip references if id's don't match
        final Label sharedLabel = outLabels.get(pos);
        if (inLabel.getId() != sharedLabel.getId()) {
          updateLabelId(inClips, sharedLabel);
        }
      }
    }

    // merge clipItems
    for (ClipItem inClip : inClips) {
      final int pos = outClips.indexOf(inClip);
      if (pos == -1) {
        // new clip - add to outgoing
        if (inClip.getDevice()
          .equals(MyDevice.INST(context).getDisplayName())) {
          inClip.setRemote(false);
        } else {
          inClip.setRemote(true);
        }
        final ClipItem outClip =
          new ClipItem(inClip, inClip.getLabels(), inClip.getLabelsId());
        outClips.add(outClip);
      } else {
        // shared clip - merge into outgoing clip
        final ClipItem outClip = outClips.get(pos);
        if (inClip.getFav()) {
          // true fav has priority
          outClip.setFav(true);
        }
        if (inClip.getDate() > outClip.getDate()) {
          // newest has priority
          outClip.setDate(inClip.getDate());
          if (inClip.getDevice()
            .equals(MyDevice.INST(context).getDisplayName())) {
            outClip.setRemote(false);
          } else {
            outClip.setRemote(true);
          }
          outClip.setDevice(inClip.getDevice());
        }
        // merge labels and labelsId
        outClip.addLabels(inClip.getLabels());
      }
    }
  }

  /**
   * Replace the contents of the database
   */
  public void replaceDB() {
    App.getExecutors().diskIO().execute(() -> {
      MainDB.INST(App.INST()).runInTransaction(() -> {
        MainDB.INST(App.INST()).labelDao().deleteAll();
        MainDB.INST(App.INST()).clipDao().deleteAll();

        MainDB.INST(App.INST()).labelDao().insertAll(labels);
        // get mapping between name and id
        final List<Label> newLabels =
          MainDB.INST(App.INST()).labelDao().getAllSync();
        final Map<String, Long> labelsMap = new HashMap<>(newLabels.size());
        for (final Label label : newLabels) {
          labelsMap.put(label.getName(), label.getId());
        }

        for (ClipItem clip : clipItems) {
          final long clipId = MainDB.INST(App.INST()).clipDao().insert(clip);
          final List<Label> clipLabels = clip.getLabels();
          for (Label label : clipLabels) {
            ClipLabelJoin join =
              new ClipLabelJoin(clipId, labelsMap.get(label.getName()));
            MainDB.INST(App.INST()).clipLabelJoinDao().insert(join);
          }
        }
      });
    });
  }
}
