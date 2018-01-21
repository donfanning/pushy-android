/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.google.common.base.Objects;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.LabelNew;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** ViewModel for a {@link LabelEntity} */
public class LabelViewModel extends AndroidViewModel {
  /** Our Repo */
  private final MainRepo mRepo;

  /** Our Label */
  private final MutableLiveData<LabelEntity> label;

  /** Original name of our Label */
  private final MutableLiveData<String> originalName;

  public LabelViewModel(@NonNull Application app, LabelEntity theLabel) {
    super(app);
    mRepo = MainRepo.INST(app);

    label = new MutableLiveData<>();
    label.setValue(theLabel);

    originalName = new MutableLiveData<>();
    originalName.setValue(label.getValue().getName());

    label.observeForever((labelEntity) -> {
      Log.logD("labelname", labelEntity.getName());
      Log.logD("orignalname", LabelViewModel.this.getOriginalName().getValue());
      setOriginalName(labelEntity.getName());
    });
  }

  public MutableLiveData<LabelEntity> getLabel() {
    return label;
  }

  public MutableLiveData<String> getOriginalName() {
    return originalName;
  }

  public void setName(String name) {
    mRepo.updateLabelAsync(name, originalName.getValue());
  }

  public void setOriginalName(String originalName) {
    this.originalName.setValue(originalName);
  }
}
