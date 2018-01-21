/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** ViewModel for a {@link LabelEntity} */
public class LabelViewModel extends AndroidViewModel {
  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Our Repo */
  private final MainRepo mRepo;

  /** Our Label */
  private final MutableLiveData<LabelEntity> label = new MutableLiveData<>();

  /** Our Label name */
  private final MutableLiveData<String> name = new MutableLiveData<>();

  /** Original name of our Label */
  private final MutableLiveData<String> originalName  = new MutableLiveData<>();

  public LabelViewModel(@NonNull Application app, LabelEntity theLabel) {
    super(app);
    mRepo = MainRepo.INST(app);

    label.setValue(theLabel);

    name.setValue(theLabel.getName());

    originalName.setValue(theLabel.getName());

    label.observeForever((labelEntity) -> {
      Log.logD(TAG, "label changed: " + labelEntity.getName());
      setOriginalName(labelEntity.getName());
    });
  }

  public MutableLiveData<LabelEntity> getLabel() {
    return label;
  }

  public MutableLiveData<String> getName() {
    return name;
  }

  public MutableLiveData<String> getOriginalName() {
    return originalName;
  }

  public void setName(String name) {
    mRepo.updateLabelAsync(name, originalName.getValue());
  }

  private void setOriginalName(String originalName) {
    this.originalName.setValue(originalName);
  }
}
