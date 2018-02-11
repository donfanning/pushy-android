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

import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.util.List;

/** ViewModel for selecting Labels for a Clip */
public class LabelsSelectViewModel extends BaseRepoViewModel<MainRepo> {
  /** Our Clip */
  @NonNull
  private final MutableLiveData<Clip> clip;

  /** Our Clip's labels */
  @NonNull
  private MediatorLiveData<List<Label>> clipLabels;

  /** Full Labels list */
  @NonNull
  private final MediatorLiveData<List<Label>> labels;

  public LabelsSelectViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    clip = new MutableLiveData<>();
    clip.setValue(null);

    clipLabels = new MediatorLiveData<>();
    clipLabels.setValue(null);

    labels = new MediatorLiveData<>();
    labels.setValue(mRepo.getLabels().getValue());
    labels.addSource(mRepo.getLabels(), labels::setValue);
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setErrorMsg(null);
  }

  @NonNull
  public LiveData<List<Label>> getLabels() {
    return labels;
  }

  @NonNull
  public LiveData<Clip> getClip() {
    return clip;
  }

  @NonNull
  public LiveData<List<Label>> getClipLabels() {
    return clipLabels;
  }

  public void setClip(@NonNull Clip clip) {
    this.clip.setValue(clip);
    clipLabels.addSource(mRepo.getLabelsForClip(clip), clipLabels::setValue);
  }
}
