/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** ViewModel for a {@link LabelEntity} */
public class LabelViewModel extends BaseRepoViewModel<MainRepo> {
  /** Our Label */
  private final MutableLiveData<LabelEntity> label;

  /** Our Label name */
  private final MutableLiveData<String> name;

  /** Original name of our Label */
  public final String originalName;

  public LabelViewModel(@NonNull Application app,
                        @NonNull LabelEntity label) {
    super(app, MainRepo.INST(app));

    this.label = new MutableLiveData<>();
    this.label.setValue(label);

    name = new MutableLiveData<>();
    name.setValue(label.getName());

    originalName = label.getName();
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

  public void changeName(String name, String oldName) {
    mRepo.updateLabel(name, oldName);
  }

  public void resetName() {
    name.setValue(originalName);
  }
}
