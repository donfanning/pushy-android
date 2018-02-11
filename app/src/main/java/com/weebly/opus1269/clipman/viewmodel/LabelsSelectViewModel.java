/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.util.List;

/** ViewModel for selecting Labels for a Clip */
public class LabelsSelectViewModel extends BaseRepoViewModel<MainRepo> {
  /** Our Clip */
  @NonNull
  private final MutableLiveData<Clip> clip;

  /** Full Labels list */
  @NonNull
  private final MediatorLiveData<List<Label>> labels;

  /** Our Clip's labels */
  @NonNull
  private final MediatorLiveData<List<Label>> clipLabels;

  /** Clip labels source */
  @Nullable
  private LiveData<List<Label>> clipLabelsSource;

  public LabelsSelectViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    clip = new MutableLiveData<>();
    clip.setValue(null);

    clipLabels = new MediatorLiveData<>();
    clipLabels.setValue(null);
    clipLabelsSource = null;

    labels = new MediatorLiveData<>();
    labels.setValue(mRepo.getLabels().getValue());
    labels.addSource(mRepo.getLabels(), labels::setValue);
  }

  @NonNull
  public LiveData<List<Label>> getLabels() {
    return labels;
  }

  public void setClip(@NonNull Clip clip) {
    this.clip.setValue(clip);
    // update Labels source too
    if (clipLabelsSource != null) {
      clipLabels.removeSource(clipLabelsSource);
    }
    clipLabelsSource = mRepo.getLabelsForClip(clip);
    clipLabels.addSource(clipLabelsSource, clipLabels::setValue);
  }

  @NonNull
  public LiveData<List<Label>> getClipLabels() {
    return clipLabels;
  }

  /**
   * Is the Label with the given name a member of our Clip
   * @param labelName Label name
   * @return true if member of Clip
   */
  public boolean hasLabel(@Nullable String labelName) {
    boolean ret = false;
    final List<Label> labels = getClipLabels().getValue();
    if (!AppUtils.isEmpty(labels)) {
      for (Label label : labels) {
        if (label.getName().equals(labelName)) {
          ret = true;
          break;
        }
      }
    }
    return ret;
  }

  /**
   * Add or remove a Label from our Clip
   * @param label Label
   * @param add   if true add, otherwise remove
   */
  public void addOrRemoveLabel(@NonNull Label label, boolean add) {
    final Clip clip = this.clip.getValue();
    if (clip != null) {
      if (add) {
        mRepo.addLabelForClip(clip, label);
      } else {
        mRepo.removeLabelForClip(clip, label);
      }
    }
  }
}
