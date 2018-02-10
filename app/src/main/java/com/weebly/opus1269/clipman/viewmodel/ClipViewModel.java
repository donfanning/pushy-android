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
import android.arch.lifecycle.MediatorLiveData;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** ViewModel for a {@link Clip} */
public class ClipViewModel extends AndroidViewModel {
  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Our Repo */
  private final MainRepo mRepo;

  /** Our Clip */
  @NonNull
  private final MediatorLiveData<Clip> clip;

  public ClipViewModel(@NonNull Application app, Clip clip) {
    super(app);

    mRepo = MainRepo.INST(app);

    this.clip = new MediatorLiveData<>();
    this.clip.setValue(null);
    this.clip.addSource(mRepo.getClip(clip.getId()), this.clip::setValue);
  }

  public LiveData<Clip> getClip() {
    return clip;
  }

  public Clip getClipSync() {
    return clip.getValue();
  }

  public void changeFav(boolean state) {
    Clip clip = getClipSync();
    if (clip != null) {
      clip.setFav(state);
      mRepo.updateClipFav(clip);
    }
  }
}
