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
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Class for the contents of a backup */
public class BackupContents {
  private List<Label> labels;
  private List<Clip> clips;

  public BackupContents() {
    this.labels = new ArrayList<>(0);
    this.clips = new ArrayList<>(0);
  }

  private BackupContents(@NonNull List<Label> labels,
                         @NonNull List<Clip> clips) {
    this.labels = labels;
    this.clips = clips;
  }

  /**
   * Get the database data
   * @return database contents
   */
  @NonNull
  public static BackupContents getDB() {
    List<Clip> clips = MainRepo.INST(App.INST()).getClipsSync();
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
    final Gson gson = new Gson();
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
      long id = label.getId();
      ret = (id > ret) ? id : ret;
    }
    return ret;
  }

  /**
   * Update the id for the {@link Label} in all the {@link Clip} objects
   * @param clips clips list
   * @param label     label to chage
   */
  private static void updateLabelId(@NonNull List<Clip> clips,
                                    @NonNull Label label) {
    for (Clip clipItem : clips) {
      clipItem.updateLabelId(label);
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
  public List<Clip> getClips() {
    return clips;
  }

  public void setClips(@NonNull List<Clip> clips) {
    this.clips = clips;
  }

  /**
   * Get our contents as a JSON string
   * @return Stringified contents
   */
  public String getAsJSON() {
    String ret;
    // stringify it
    final Gson gson = new Gson();
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
    final List<Clip> outClips = this.clips;

    // Items to be merged
    final List<Label> inLabels = contents.getLabels();
    final List<Clip> inClips = contents.getClips();

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

    // merge clips
    for (Clip inClip : inClips) {
      final int pos = outClips.indexOf(inClip);
      if (pos == -1) {
        // new clip - add to outgoing
        if (inClip.getDevice()
          .equals(MyDevice.INST(context).getDisplayName())) {
          inClip.setRemote(false);
        } else {
          inClip.setRemote(true);
        }
        final Clip outClip =
          new Clip(inClip, inClip.getLabels(), inClip.getLabelsId());
        outClips.add(outClip);
      } else {
        // shared clip - merge into outgoing clip
        final Clip outClip = outClips.get(pos);
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
}
