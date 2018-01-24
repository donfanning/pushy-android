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
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.model.Prefs;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/** Singleton - Repository for {@link LabelEntity} objects */
public class MainRepo extends BaseRepo {
  @SuppressLint("StaticFieldLeak")
  private static MainRepo sInstance;

  /** Database */
  private final MainDB mDB;

  /** Clip list */
  private final MediatorLiveData<List<ClipEntity>> clipsList;

  /** Label list */
  private final MediatorLiveData<List<LabelEntity>> labelsList;

  private MainRepo(final Application app) {
    super(app);

    mDB = MainDB.INST(app);

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

  public MutableLiveData<List<ClipEntity>> getClips() {
    return clipsList;
  }

  public MutableLiveData<List<LabelEntity>> getLabels() {
    return labelsList;
  }

  private void postLabels(@NonNull List<LabelEntity> labels) {
    sortLabels(labels);
    this.labelsList.postValue(labels);
  }

  public void addClipAsync(@NonNull ClipEntity clip, boolean onNewOnly) {
    if (onNewOnly) {
      addClipIfNewAsync(clip);
    } else {
      addClipAsync(clip);
    }
  }

  /**
   * Insert or replace a clip
   * @param clip Clip to insert or replace
   */
  public void addClipAsync(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).clipDao().insertAll(clip));
  }

  /**
   * Insert a clip only if it does not exist
   * @param clip Clip to insert or replace
   */
  public void addClipIfNewAsync(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).clipDao().insertIfNew(clip));
  }

  /**
   * Add a clip but preserve the true fav state if it exists
   * @param clip Clip to insert or replace
   */
  public void addClipKeepTrueFav(@NonNull ClipEntity clip) {
    ClipEntity existingClip =
      MainDB.INST(mApp).clipDao().getClipWithTrueFavSync(clip.getText());
    if ((existingClip != null) && existingClip.getFav()) {
      clip.setFav(true);
    }
    MainDB.INST(App.INST()).clipDao().insert(clip);
  }

  public void removeClipAsync(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).clipDao().delete(clip));
  }

  public void addClipAndSendAsync(Context cntxt, ClipEntity clip,
                                  boolean onNewOnly) {
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

  public LiveData<LabelEntity> getLabelAsync(String name) {
    return MainDB.INST(mApp).labelDao().getLabel(name);
    //App.getExecutors().diskIO()
    //  .execute(() -> MainDB.INST(mApp).labelDao().getLabel(name));
  }

  public void addLabelAsync(@NonNull LabelEntity label) {
    App.getExecutors().diskIO()
      .execute(() -> MainDB.INST(mApp).labelDao().insertAll(label));
  }

  public void updateLabelAsync(@NonNull String newName,
                               @NonNull String oldName) {
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
