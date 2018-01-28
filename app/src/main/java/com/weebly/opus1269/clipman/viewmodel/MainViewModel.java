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

import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.util.List;

/** ViewModel for MainActvity */
public class MainViewModel extends BaseRepoViewModel<MainRepo> {
  /** Clips list */
  private final MediatorLiveData<List<ClipEntity>> clips;

  public MainViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    clips = new MediatorLiveData<>();
    clips.setValue(null);
    clips.addSource(mRepo.loadClips(), clips::setValue);
  }

  public LiveData<List<ClipEntity>> loadClips() {
    return clips;
  }
}
