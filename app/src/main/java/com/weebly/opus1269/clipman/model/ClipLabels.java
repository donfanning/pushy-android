/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;


import com.weebly.opus1269.clipman.db.LabelTables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Collection of {@link Label} objects for a {@link ClipItem} */
public class ClipLabels implements Serializable {

  /** List of Labels */
  private List<Label> mLabels;

  private ClipLabels(ArrayList<Label> labels) {mLabels = labels;}

  public ClipLabels() {mLabels = new ArrayList<>(0);}

  /**
   * Load List from db
   * @return true if we have at least one Label
   */
  public static ClipLabels load(ClipItem clipItem) {
    ArrayList<Label> labels = LabelTables.INST.getLabels(clipItem);
    return new ClipLabels(labels);
  }

  /**
   * Get number of {@link Label} objects
   * @return count
   */
  public int size() {return mLabels.size();}

  /**
   * Get the List of Labels
   * @return List
   */
  public List<Label> get() {return mLabels;}

  /**
   * Does the {@link Label} exist
   * @return List
   */
  public boolean contains(Label label) {return mLabels.contains(label);}

  /**
   * Add a new {@link Label}
   * @param labels List of labels
   */
  public void add(List<Label> labels) {mLabels = labels;}

  /**
   * Add a new {@link Label}
   * @param label a label
   * @return true if Label does not exist
   */
  public boolean add(Label label) {
    if (mLabels.contains(label)) {
      return false;
    }
    mLabels.add(label);
    return true;
  }

  /**
   * Remove {@link Label}
   * @param label a label
   * @return true if removed
   */
  public boolean remove(Label label) {return mLabels.remove(label);}
}

