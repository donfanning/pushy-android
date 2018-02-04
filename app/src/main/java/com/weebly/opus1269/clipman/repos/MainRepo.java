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

  public LiveData<List<ClipEntity>> getClips(boolean filterByFavs,
                                             boolean pinFavs, int sortType) {
    // itty bitty FSM - Room needs better way
    if (filterByFavs) {
      if (sortType == 1) {
        return mDB.clipDao().getFavsByText();
      }
      return mDB.clipDao().getFavs();
    }
    if (pinFavs) {
      if (sortType == 1) {
        return mDB.clipDao().getAllPinFavsByText();
      }
      return mDB.clipDao().getAllPinFavs();
    }
    if (sortType == 1) {
      return mDB.clipDao().getAllByText();
    }
    return mDB.clipDao().getAll();
  }

  public List<ClipEntity> getClipsSync() {
    return mDB.clipDao().getAllSync();
  }

  public List<ClipEntity> getNonFavClipsSync() {
    return mDB.clipDao().getNonFavsSync();
  }

  public LiveData<List<LabelEntity>> getLabels() {
    return labelsList;
  }

  public LiveData<ClipEntity> getClip(final long id) {
    return mDB.clipDao().get(id);
  }

  /**
   * Insert or replace a list of clips
   * @param clips Clips list
   */
  public void addClips(@NonNull List<ClipEntity> clips) {
    setIsWorking(true);
    App.getExecutors().diskIO().execute(() -> {
      final long id[] = mDB.clipDao().insertAll(clips);
      Log.logD(TAG, "added clips: " + id.length);
      postIsWorking(false);
    });
  }

  /**
   * Insert or replace a clip
   * @param clip Clip to insert or replace
   */
  public void addClip(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO().execute(() -> {
      postIsWorking(true);
      final long id = mDB.clipDao().insert(clip);
      Log.logD(TAG, "added id: " + id);
      postInfoMessage("Added clip");
      postIsWorking(false);
    });
  }

  /**
   * Insert a clip only if the text does not exist
   * @param clip Clip to insert
   */
  public void addClipIfNew(@NonNull ClipEntity clip) {
    addClipIfNew(clip, false);
  }

  /**
   * Insert a clip only if the text does not exist
   * @param clip   Clip to insert
   * @param silent if true, no messages
   */
  public void addClipIfNew(@NonNull ClipEntity clip, boolean silent) {
    App.getExecutors().diskIO().execute(() -> {
      postIsWorking(true);
      if (mDB.clipDao().getSync(clip.getText()) == null) {
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
   * Insert or update clip and optionally send to remote devices
   * @param clip      The Clip
   * @param onNewOnly if true, only add if it doesn't exist
   */
  public void addClipAndSend(ClipEntity clip, boolean onNewOnly) {
    App.getExecutors().diskIO().execute(() -> {
      postIsWorking(true);
      final Context context = mApp;
      long id = -1L;
      int nRows = 0;
      if (onNewOnly) {
        id = mDB.clipDao().insertIfNew(clip);
      } else {
        final ClipEntity existingClip = mDB.clipDao().getSync(clip.getText());
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

  /**
   * Insert or replace clip but preserve fav state if true
   * @param clip Clip to insert or replace
   */
  public long addClipKeepTrueFavSync(@NonNull ClipEntity clip) {
    ClipEntity existingClip = mDB.clipDao().getIfTrueFavSync(clip.getText());
    if ((existingClip != null) && existingClip.getFav()) {
      clip.setFav(true);
    }
    return MainDB.INST(App.INST()).clipDao().insert(clip);
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

  public void updateClipFav(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.clipDao().updateFav(clip.getText(), clip.getFav()));
  }

  public void removeClip(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.clipDao().delete(clip));
  }

  public List<ClipEntity> removeAllClipsSync(boolean includeFavs) {
    final List<ClipEntity> ret;
    if (includeFavs) {
      ret = mDB.clipDao().getAllSync();
      mDB.clipDao().deleteAll();
    } else {
      ret = mDB.clipDao().getNonFavsSync();
      mDB.clipDao().deleteAllNonFavs();
    }
    return ret;
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
