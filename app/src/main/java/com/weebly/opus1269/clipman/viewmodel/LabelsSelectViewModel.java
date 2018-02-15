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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.ClipItem;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.util.List;

/** ViewModel for selecting Labels for a ClipItem */
public class LabelsSelectViewModel extends BaseRepoViewModel<MainRepo> {
  /** Our ClipItem */
  @NonNull
  private final MediatorLiveData<ClipItem> clip;

  /** Full Labels list */
  @NonNull
  private final MediatorLiveData<List<Label>> labels;

  public LabelsSelectViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    clip = new MediatorLiveData<>();
    clip.setValue(null);

    labels = new MediatorLiveData<>();
    labels.setValue(mRepo.getLabels().getValue());
    labels.addSource(mRepo.getLabels(), labels::setValue);
  }

  @NonNull
  public LiveData<ClipItem> getClip() {
    return clip;
  }

  public void setClip(@NonNull ClipItem aClip) {
    this.clip.addSource(mRepo.getClip(aClip.getId()),
      clip -> App.getExecutors().diskIO().execute(() -> {
        if (clip != null) {
          clip.setLabels(mRepo.getLabelsForClipSync(clip));
          this.clip.postValue(clip);
        }
      }));
  }

  @NonNull
  public LiveData<List<Label>> getLabels() {
    return labels;
  }

  /**
   * Is the Label with the given name a member of our ClipItem
   * @param labelName Label name
   * @return true if member of ClipItem
   */
  public boolean hasLabel(@Nullable String labelName) {
    boolean ret = false;
    final ClipItem clip = this.clip.getValue();
    if (clip != null) {
      final List<Label> labels = clip.getLabels();
      if (!AppUtils.isEmpty(labels)) {
        for (Label label : labels) {
          if (label.getName().equals(labelName)) {
            ret = true;
            break;
          }
        }
      }
    }
    return ret;
  }

  /**
   * Add or remove a Label from our ClipItem
   * @param label Label
   * @param add   if true add, otherwise remove
   */
  public void addOrRemoveLabel(@NonNull Label label, boolean add) {
    final ClipItem clip = this.clip.getValue();
    if (clip != null) {
      App.getExecutors().diskIO().execute(() -> {
        if (add) {
          mRepo.addLabelForClipSync(clip, label);
          clip.addLabel(label);
        } else {
          mRepo.removeLabelForClipSync(clip, label);
          clip.removeLabel(label);
        }
        // force update so labels get processed
        mRepo.updateClipSync(clip);
      });
    }
  }
}
