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
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    clipsList.addSource(mDB.clipDao().loadAll(), clips -> {
      if (mDB.getDatabaseCreated().getValue() != null) {
        clipsList.postValue(clips);
      }
    });

    labelsList = new MediatorLiveData<>();
    labelsList.postValue(new ArrayList<>());
    labelsList.addSource(mDB.labelDao().loadAll(), labels -> {
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

  public LiveData<List<ClipEntity>> loadClips() {
    return clipsList;
  }

  public LiveData<List<LabelEntity>> loadLabels() {
    return labelsList;
  }

  public LiveData<ClipEntity> loadClip(final String text) {
    return mDB.clipDao().load(text);
  }

  @Nullable
  public ClipEntity getClip(final String text) {
    return mDB.clipDao().get(text);
  }

  public LiveData<LabelEntity> loadLabel(final String name) {
    return mDB.labelDao().load(name);
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
      .execute(() -> {
        final long row = mDB.clipDao().insert(clip);
        Log.logD(TAG, "add, row: " + row);
      });
  }

  /**
   * Insert a clip only if it does not exist
   * @param clip Clip to insert or replace
   */
  public void addClipIfNewAsync(@NonNull ClipEntity clip) {
    postIsWorking(true);
    App.getExecutors().diskIO().execute(() -> {
      if (mDB.clipDao().get(clip.getText()) == null) {
        long row = mDB.clipDao().insert(clip);
        if (row == -1L) {
          errorMsg.postValue(new ErrorMsg("insert failed"));
        } else {
          errorMsg.postValue(null);
        }
        postIsWorking(false);
      } else {
        errorMsg.postValue(new ErrorMsg("clip exists"));
      }
    });
  }

  /**
   * Add a clip but preserve the true fav state if it exists
   * @param clip Clip to insert or replace
   */
  public void addClipKeepTrueFav(@NonNull ClipEntity clip) {
    ClipEntity existingClip = mDB.clipDao().getIfTrueFav(clip.getText());
    if ((existingClip != null) && existingClip.getFav()) {
      clip.setFav(true);
    }
    MainDB.INST(App.INST()).clipDao().insert(clip);
  }

  /**
   * Insert a clip only if it does not exist and copy to clipboard
   * @param clip Clip to insert and copy
   */
  public void addClipIfNewAndCopyAsync(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO().execute(() -> {
      postIsWorking(true);
      if (mDB.clipDao().get(clip.getText()) == null) {
        if (!Prefs.INST(mApp).isMonitorClipboard()) {
          // if not monitoring clipboard, need to handle insert ourselves
          // otherwise the ClipWatcher will handle it
          int nRows = mDB.clipDao().delete(clip);
          Log.logD(TAG, "deleted rows: " + nRows);
          Log.logD(TAG, "current row: " + clip.getId());
          long row = mDB.clipDao().insert(clip);
          Log.logD(TAG, "insert row: " + row);
          if (row == -1L) {
            errorMsg.postValue(new ErrorMsg("insert failed"));
          } else {
            errorMsg.postValue(null);
          }
        }
        clip.copyToClipboard(mApp);
      } else {
        errorMsg.postValue(new ErrorMsg("clip exists"));
      }
      postIsWorking(false);
    });
  }


  public void updateFavAsync(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.clipDao().updateFav(clip.getText(), clip.getFav()));
  }

  public void removeClipAsync(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.clipDao().delete(clip));
  }

  public void addClipAndSendAsync(Context cntxt, ClipEntity clip,
                                  boolean onNewOnly) {
    App.getExecutors().diskIO().execute(() -> {
      postIsWorking(true);
      long id;
      if (onNewOnly) {
        id = mDB.clipDao().insertIfNew(clip);
      } else {
        id = mDB.clipDao().insert(clip);
      }

      Log.logD(TAG, "addClipAndSendAsync id: " + id);

      if (id != -1L) {
        Notifications.INST(cntxt).show(clip);

        if (!clip.getRemote() && Prefs.INST(cntxt).isAutoSend()) {
          clip.send(cntxt);
        }
      }
      postIsWorking(false);
    });
  }

  public LiveData<LabelEntity> getLabelAsync(String name) {
    return mDB.labelDao().load(name);
    //App.getExecutors().diskIO()
    //  .execute(() -> mDB.labelDao().getLabel(name));
  }

  public void addLabelAsync(@NonNull LabelEntity label) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.labelDao().insertAll(label));
  }

  public void updateLabelAsync(@NonNull String newName,
                               @NonNull String oldName) {
    App.getExecutors().diskIO().execute(() -> {
      final int nRows = mDB.labelDao().updateName(newName, oldName);
      if (nRows == 0) {
        postErrorMsg(new ErrorMsg("Label exists"));
      }
    });
  }

  public void removeLabelAsync(@NonNull LabelEntity label) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.labelDao().delete(label));
  }

  /**
   * Sort list of labelsList in place - alphabetical
   * @param labels List to sort
   */
  private void sortLabels(@NonNull List<LabelEntity> labels) {
    java.util.Collections.sort(labels, Collator.getInstance());
  }
}
