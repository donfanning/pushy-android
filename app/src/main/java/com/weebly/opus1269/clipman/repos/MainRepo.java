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

import java.util.ArrayList;
import java.util.List;

/** Singleton - Repository for {@link LabelEntity} objects */
public class MainRepo extends BaseRepo {
  @SuppressLint("StaticFieldLeak")
  private static MainRepo sInstance;

  /** Database */
  private final MainDB mDB;

  /** Label list */
  private final MediatorLiveData<List<LabelEntity>> labelsList;

  private MainRepo(final Application app) {
    super(app);

    mDB = MainDB.INST(app);

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

  public LiveData<List<ClipEntity>> loadClips(boolean filterByFavs,
                                              boolean pinFavs, int sortType) {
    // itty bitty FSM - Room needs better way
    if (filterByFavs) {
      if (sortType == 1) {
        return mDB.clipDao().loadFavsByText();
      }
      return mDB.clipDao().loadFavs();
    }
    if (pinFavs) {
      if (sortType == 1) {
        return mDB.clipDao().loadAllPinFavsByText();
      }
      return mDB.clipDao().loadAllPinFavs();
    }
    if (sortType == 1) {
      return mDB.clipDao().loadAllByText();
    }
    return mDB.clipDao().loadAll();
  }

  public LiveData<List<LabelEntity>> loadLabels() {
    return labelsList;
  }

  public LiveData<ClipEntity> loadClip(final long id) {
    return mDB.clipDao().load(id);
  }

  @Nullable
  public ClipEntity getClip(final String text) {
    return mDB.clipDao().get(text);
  }

  /**
   * Insert or replace a clip
   * @param clip Clip to insert or replace
   */
  public void addClip(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO().execute(() -> {
      postIsWorking(true);
      final long row = mDB.clipDao().insert(clip);
      Log.logD(TAG, "add, row: " + row);
      postInfoMessage("Added clip");
      postIsWorking(false);
    });
  }

  /**
   * Insert a clip only if the text does not exist
   * @param clip Clip to insert or replace
   */
  public void addClipIfNew(@NonNull ClipEntity clip) {
    addClipIfNew(clip, false);
  }

  /**
   * Insert a clip only if the text does not exist
   * @param clip   Clip to insert or replace
   * @param silent if true, no messages
   */
  public void addClipIfNew(@NonNull ClipEntity clip, boolean silent) {
    App.getExecutors().diskIO().execute(() -> {
      postIsWorking(true);
      if (mDB.clipDao().get(clip.getText()) == null) {
        long row = mDB.clipDao().insert(clip);
        if (row == -1L) {
          if (!silent) {
            errorMsg.postValue(new ErrorMsg("Add failed"));
          }
        } else {
          // success
          if (!silent) {
            infoMessage.postValue("Clip added");
            errorMsg.postValue(null);
          }
        }
      } else if (!silent) {
        errorMsg.postValue(new ErrorMsg("Clip exists"));
      }
      postIsWorking(false);
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

  public void updateFav(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.clipDao().updateFav(clip.getText(), clip.getFav()));
  }

  public void removeClip(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.clipDao().delete(clip));
  }

  /**
   * Insert or update clip and optionally send to remote devices
   * @param clip      The Clip
   * @param onNewOnly if true, only add if it doesn't exist
   */
  public void addClipAndSend(ClipEntity clip, boolean onNewOnly) {
    App.getExecutors().diskIO().execute(() -> {
      final Context context = mApp;
      postIsWorking(true);
      long id = -1L;
      int nRows = 0;
      if (onNewOnly) {
        id = mDB.clipDao().insertIfNew(clip);
      } else {
        final ClipEntity existingClip = mDB.clipDao().get(clip.getText());
        if (existingClip != null) {
          // clip exists, update it
          clip.setId(existingClip.getId());
          nRows = mDB.clipDao().update(clip);
        } else {
          id = mDB.clipDao().insert(clip);
        }
      }

      if (id != -1L || nRows != 0) {
        // success
        if (id != -1L) {
          Log.logD(TAG, "addClipAndSendAsync added id: " + id);
        } else {
          Log.logD(TAG, "addClipAndSendAsync updated id: " + clip.getId());
        }

        Notifications.INST(context).show(clip);

        if (!clip.getRemote() && Prefs.INST(context).isAutoSend()) {
          clip.send(context);
        }
      }
      postIsWorking(false);
    });
  }

  public void updateClip(@NonNull ClipEntity clipEntity) {
    App.getExecutors().diskIO().execute(() -> {
      final int nRows = mDB.clipDao().update(clipEntity);
      if (nRows == 0) {
        postErrorMsg(new ErrorMsg("Clip exists"));
      } else {
        postInfoMessage("Clip Updated");
      }
    });
  }

  public void addLabelIfNew(@NonNull LabelEntity label) {
    App.getExecutors().diskIO().execute(() -> {
      final long id = mDB.labelDao().insertIfNew(label);
      if (id == -1L) {
        errorMsg.postValue(new ErrorMsg("Label exists"));
      }
    });
  }

  public void updateLabel(@NonNull String newName,
                          @NonNull String oldName) {
    App.getExecutors().diskIO().execute(() -> {
      final int nRows = mDB.labelDao().updateName(newName, oldName);
      if (nRows == 0) {
        postErrorMsg(new ErrorMsg("Label exists"));
      }
    });
  }

  public void removeLabel(@NonNull LabelEntity label) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.labelDao().delete(label));
  }
}
