/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.util.List;

/** ViewModel for Labels */
public class LabelsViewModel extends AndroidViewModel {
  /** Error message */
  private final MediatorLiveData<ErrorMsg> errorMsg;

  /** True if loading */
  private final MediatorLiveData<Boolean> isLoading;

  /** Labels list */
  private final MediatorLiveData<List<LabelEntity>> labels;

  public LabelsViewModel(@NonNull Application app) {
    super(app);

    MainRepo repo = MainRepo.INST(app);

    errorMsg = new MediatorLiveData<>();
    errorMsg.setValue(repo.getErrorMsg().getValue());
    errorMsg.addSource(repo.getErrorMsg(), errorMsg::setValue);

    isLoading = new MediatorLiveData<>();
    isLoading.setValue(repo.getIsLoading().getValue());
    isLoading.addSource(repo.getIsLoading(), isLoading::setValue);

    labels = new MediatorLiveData<>();
    labels.setValue(repo.loadLabels().getValue());
    labels.addSource(repo.loadLabels(), labels::setValue);
  }

  @NonNull
  public MutableLiveData<ErrorMsg> getErrorMsg() {
    return errorMsg;
  }

  public MutableLiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public MutableLiveData<List<LabelEntity>> getLabels() {
    return labels;
  }
}
