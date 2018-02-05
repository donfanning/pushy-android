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
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** ViewModel for a {@link LabelEntity} */
public class LabelViewModel extends BaseRepoViewModel<MainRepo> {
  /** Our Label */
  private final MediatorLiveData<LabelEntity> label;

  /** Our Label name */
  private final MutableLiveData<String> name;

  /** Original name of our Label */
  private String originalName;

  public LabelViewModel(@NonNull Application app,
                        @NonNull LabelEntity aLabel) {
    super(app, MainRepo.INST(app));

    name = new MutableLiveData<>();
    name.setValue(aLabel.getName());

    originalName = aLabel.getName();

    label = new MediatorLiveData<>();
    label.setValue(aLabel);
    label.addSource(mRepo.getLabel(aLabel.getId()), label -> {
      this.label.setValue(label);
      final String name = label == null ? "" : label.getName();
        this.name.setValue(name);
        originalName = name;
    });
  }

  @Override
  protected void initRepo() {
    super.initRepo();
    mRepo.setErrorMsg(null);
  }

  public LiveData<LabelEntity> getLabel() {
    return label;
  }

  public MutableLiveData<String> getName() {
    return name;
  }

  public String getOriginalName() {
    return originalName;
  }

  public void updateLabel() {
    String name = this.name.getValue();
    if (!AppUtils.isWhitespace(name)) {
      name = name.trim();
      if (!TextUtils.equals(name, originalName)) {
        // update label
        mRepo.updateLabel(name, originalName);
      } else {
        resetName();
      }
    } else {
      resetName();
    }
  }

  public void resetName() {
    name.setValue(originalName);
  }
}
