/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.repos;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.MainDB;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.model.Prefs;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/** Singleton - Repository for {@link LabelEntity} objects */
public class MainRepo {
  @SuppressLint("StaticFieldLeak")
  private static MainRepo sInstance;

  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Application */
  private final Application mApp;

  /** Database */
  private final MainDB mDB;

  /** Error message */
  private final MutableLiveData<ErrorMsg> errorMsg;

  /** True if loading */
  private final MutableLiveData<Boolean> isLoading;
  
  /** Clip list */
  private final MediatorLiveData<List<ClipEntity>> clipsList;

  /** Label list */
  private final MediatorLiveData<List<LabelEntity>> labelsList;

  private MainRepo(final Application app) {
    mApp = app;
    mDB = MainDB.INST(app);

    errorMsg = new MutableLiveData<>();
    errorMsg.postValue(null);

    isLoading = new MutableLiveData<>();
    isLoading.postValue(false);

    clipsList = new MediatorLiveData<>();
    clipsList.postValue(new ArrayList<>());
    clipsList.addSource(mDB.clipDao().getAll(), clips -> {
      if (mDB.getDatabaseCreated().getValue() != null) {
        clipsList.postValue(clips);
      }
    });

    labelsList = new MediatorLiveData<>();
    labelsList.postValue(new ArrayList<>());
    labelsList.addSource(mDB.labelDao().getAll(), labels -> {
      if (mDB.getDatabaseCreated().getValue() != null) {
        labelsList.postValue(labels);
      }
    });
  }

  public static MainRepo INST(final Application app) {
    if (sInstance == null) {
      synchronized (MainRepo.class) {
        if (sInstance == null) {
          sInstance = new MainRepo(app);
        }
      }
    }
    return sInstance;
  }

  public MutableLiveData<ErrorMsg> getErrorMsg() {
    return errorMsg;
  }

  public MutableLiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public MutableLiveData<List<ClipEntity>> getClips() {
    return clipsList;
  }

  public MutableLiveData<List<LabelEntity>> getLabels() {
    return labelsList;
  }

  public void postErrorMsg(ErrorMsg value) {
    errorMsg.postValue(value);
  }

  public void postIsLoading(boolean value) {
    isLoading.postValue(value);
  }

  private void postLabels(@NonNull List<LabelEntity> labels) {
    sortLabels(labels);
    this.labelsList.postValue(labels);
  }

  public LiveData<LabelEntity> getLabelAsync(String name) {
    return MainDB.INST(mApp).labelDao().getLabel(name);
    //App.getExecutors().diskIO()
    //  .execute(() -> MainDB.INST(mApp).labelDao().getLabel(name));
  }

  public void addClipAsync(@NonNull ClipEntity clip, boolean onNewOnly) {
    if (onNewOnly) {
      addClipIfNewAsync(clip);
    } else {
      addClipAsync(clip);
    }
  }

  public void addClipAsync(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).clipDao().insertAll(clip));
  }

  public void addClipIfNewAsync(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).clipDao().insertIfNew(clip));
  }

  public void removeClipAsync(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).clipDao().delete(clip));
  }

  public void addClipAndSendAsync(Context cntxt, ClipEntity clip, boolean onNewOnly) {
    App.getExecutors().diskIO().execute(() -> {
      long id;
      if (onNewOnly) {
        id = MainDB.INST(mApp).clipDao().insertIfNew(clip);
      } else {
        id = MainDB.INST(mApp).clipDao().insert(clip);
      }

      Log.logD(TAG, "id: " + id);

      if (id != -1L) {
        Notifications.INST(cntxt).show(clip);

        if (!clip.getRemote() && Prefs.INST(cntxt).isAutoSend()) {
          clip.send(cntxt);
        }
      }
    });
  }


  public void addLabelAsync(@NonNull LabelEntity label) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).labelDao().insertAll(label));
  }

  public void updateLabelAsync(@NonNull String newName, @NonNull String oldName) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).labelDao().updateName(newName, oldName));
  }

  public void removeLabelAsync(@NonNull LabelEntity label) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).labelDao().delete(label));
  }

  /**
   * Sort list of labelsList in place - alphabetical
   * @param labels List to sort
   */
  private void sortLabels(@NonNull List<LabelEntity> labels) {
    java.util.Collections.sort(labels, Collator.getInstance());
  }
}
