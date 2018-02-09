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
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.R;
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
public class MainRepo extends BaseRepo implements
  SharedPreferences.OnSharedPreferenceChangeListener {
  @SuppressLint("StaticFieldLeak")
  private static MainRepo sInstance;

  /** Database */
  private final MainDB mDB;

  /** Clips list */
  @NonNull
  private final MediatorLiveData<List<ClipEntity>> clips;

  /** Label list */
  @NonNull
  private final MediatorLiveData<List<LabelEntity>> labels;

  /** Selected clip */
  @NonNull
  private final MediatorLiveData<ClipEntity> selectedClip;

  /** Clips Source */
  @NonNull
  private LiveData<List<ClipEntity>> clipsSource;

  /** Selected clip Source */
  @Nullable
  private LiveData<ClipEntity> selectedClipSource;

  /** Sort with favorites first if true */
  private boolean pinFavs;

  /** Show only favorites if true */
  private boolean filterByFavs;

  /** Sort by date or text */
  private int sortType;

  /** Show only Clips with the given label if non-whitespace */
  @NonNull
  private String labelFilter;

  /** Text to filter clips on */
  @NonNull
  private final MutableLiveData<String> clipTextFilter;

  private MainRepo(final Application app) {
    super(app);

    mDB = MainDB.INST(app);

    pinFavs = Prefs.INST(app).isPinFav();
    filterByFavs = Prefs.INST(app).isFavFilter();
    labelFilter = Prefs.INST(app).getLabelFilter();
    sortType = Prefs.INST(app).getSortType();

    clipTextFilter = new MutableLiveData<>();
    clipTextFilter.postValue("");

    clips = new MediatorLiveData<>();
    clips.postValue(null);
    clipsSource =
      getClips(filterByFavs, pinFavs, sortType, getClipTextFilterSync());
    clips.addSource(clipsSource, clips::postValue);

    selectedClip = new MediatorLiveData<>();
    selectedClip.postValue(null);
    selectedClipSource = null;

    labels = new MediatorLiveData<>();
    labels.postValue(new ArrayList<>());
    labels.addSource(mDB.labelDao().getAll(), labels -> {
      if (mDB.getDatabaseCreated().getValue() != null) {
        this.labels.postValue(labels);
      }
    });

    // listen for preference changes
    PreferenceManager
      .getDefaultSharedPreferences(app)
      .registerOnSharedPreferenceChangeListener(this);
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

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                        String key) {
    final Context context = mApp;
    // TODO do something with labelfilter

    //noinspection IfCanBeSwitch
    if (Prefs.INST(context).PREF_FAV_FILTER.equals(key)) {
      filterByFavs = Prefs.INST(context).isFavFilter();
      final ClipEntity clip = getSelectedClipSync();
      if (filterByFavs && (clip != null) && !clip.getFav()) {
        // unselect if we will be filtered out
        setSelectedClip(null);
      }
    } else if (Prefs.INST(context).PREF_PIN_FAV.equals(key)) {
      pinFavs = Prefs.INST(context).isPinFav();
    } else if (Prefs.INST(context).PREF_SORT_TYPE.equals(key)) {
      sortType = Prefs.INST(context).getSortType();
    } else if (Prefs.INST(context).PREF_LABEL_FILTER.equals(key)) {
      labelFilter = Prefs.INST(context).getLabelFilter();
    } else {
      // not ours to handle
      return;
    }

    changeClips();
  }

  public LiveData<List<ClipEntity>> getClips() {
    return clips;
  }

  public LiveData<List<LabelEntity>> getLabels() {
    return labels;
  }

  @NonNull
  public LiveData<ClipEntity> getSelectedClip() {
    return selectedClip;
  }

  public void setSelectedClip(@Nullable ClipEntity clip) {
    if (selectedClipSource != null) {
      selectedClip.removeSource(selectedClipSource);
    }
    if (ClipEntity.isWhitespace(clip)) {
      // no clip
      selectedClipSource = null;
      selectedClip.setValue(null);
    } else {
      selectedClipSource = getClip(clip.getId());
      selectedClip.addSource(selectedClipSource, this.selectedClip::setValue);
    }
  }

  @Nullable
  public ClipEntity getSelectedClipSync() {
    return selectedClip.getValue();
  }

  public LiveData<ClipEntity> getClip(final long id) {
    return mDB.clipDao().get(id);
  }

  public LiveData<LabelEntity> getLabel(final long id) {
    return mDB.labelDao().get(id);
  }

  @NonNull
  public LiveData<String> getClipTextFilter() {
    return clipTextFilter;
  }

  @NonNull
  public String getClipTextFilterSync() {
    final String s = clipTextFilter.getValue();
    return (s == null) ? "" : s;
  }

  public void setClipTextFilter(@NonNull String s) {
    clipTextFilter.setValue(s);
    changeClips();
  }

  /**
   * Insert or replace a list of clips
   * @param clips Clip list
   */
  public void addClips(@NonNull List<ClipEntity> clips) {
    setIsWorking(true);
    App.getExecutors().diskIO().execute(() -> {
      final long id[] = mDB.clipDao().insertAll(clips);
      Log.logD(TAG, "added " + id.length + " clips");
      postIsWorking(false);
    });
  }

  /**
   * Insert or replace clip but preserve fav state if it is true
   * @param clip Clip
   */
  public void addClip(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO().execute(() -> addClipSync(clip));
  }

  /**
   * Insert or replace clip but preserve fav state if it is true
   * @param clip Clip
   */
  public long addClipSync(@NonNull ClipEntity clip) {
    final ClipEntity oldClip = mDB.clipDao().getSync(clip.getText());
    final long id = (oldClip != null) ? oldClip.getId() : clip.getId();
    final boolean fav = (oldClip != null && oldClip.getFav()) || clip.getFav();
    clip.setId(id);
    clip.setFav(fav);
    return mDB.clipDao().insert(clip);
  }

  /**
   * Insert a clip only if the text does not exist
   * @param clip Clip
   */
  public void addClipIfNew(@NonNull ClipEntity clip) {
    addClipIfNew(clip, false);
  }

  /**
   * Insert a clip only if the text does not exist
   * @param clip   Clip
   * @param silent if true, no messages
   */
  public void addClipIfNew(@NonNull ClipEntity clip, boolean silent) {
    App.getExecutors().diskIO().execute(() -> {
      if (!exists(clip)) {
        mDB.clipDao().insert(clip);
        if (!silent) {
          postInfoMessage(mApp.getString(R.string.repo_clip_added));
          errorMsg.postValue(null);
        }
      } else if (!silent) {
        postErrorMsg(new ErrorMsg(mApp.getString(R.string.repo_clip_exists)));
      }
    });
  }

  /**
   * Insert or update clip and optionally send to remote devices
   * @param clip      Clip
   * @param onNewOnly if true, only add if text doesn't exist
   */
  public void addClipAndSend(ClipEntity clip, boolean onNewOnly) {
    App.getExecutors().diskIO().execute(() -> {
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
          if (existingClip.getFav()) {
            clip.setFav(true);
          }
          nRows = mDB.clipDao().update(clip);
        } else {
          id = mDB.clipDao().insert(clip);
        }
      }

      if (id != -1L || nRows != 0) {
        // success
        if (id != -1L) {
          Log.logD(TAG, "addClipAndSend added id: " + id);
        } else {
          Log.logD(TAG, "addClipAndSend updated id: " + clip.getId());
        }

        Notifications.INST(context).show(clip);

        if (!clip.getRemote() && Prefs.INST(context).isAutoSend()) {
          clip.send(context);
        }
      }
    });
  }

  public void updateClip(@NonNull ClipEntity clipEntity) {
    App.getExecutors().diskIO().execute(() -> {
      final int nRows = mDB.clipDao().update(clipEntity);
      if (nRows == 0) {
        postErrorMsg(new ErrorMsg(mApp.getString(R.string.repo_clip_exists)));
      } else {
        postInfoMessage(mApp.getString(R.string.repo_clip_updated));
      }
    });
  }

  public void updateClipFav(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.clipDao().updateFav(clip.getText(), clip.getFav()));
  }

  public void removeClip(@NonNull ClipEntity clip) {
    App.getExecutors().diskIO().execute(() -> mDB.clipDao().delete(clip));
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
        postErrorMsg(new ErrorMsg(mApp.getString(R.string.repo_label_exists)));
      }
    });
  }

  public void updateLabel(@NonNull String newName,
                          @NonNull String oldName) {
    App.getExecutors().diskIO().execute(() -> {
      final int nRows = mDB.labelDao().updateName(newName, oldName);
      if (nRows == 0) {
        postErrorMsg(new ErrorMsg(mApp.getString(R.string.repo_label_exists)));
      }
    });
  }

  public void removeLabel(@NonNull LabelEntity label) {
    App.getExecutors().diskIO().execute(() -> mDB.labelDao().delete(label));
  }

  /**
   * Does a clip with the given text exist
   * @param clip Clip
   */
  private boolean exists(@NonNull ClipEntity clip) {
    return mDB.clipDao().getSync(clip.getText()) != null;
  }

  private LiveData<List<ClipEntity>> getClips(boolean filterByFavs,
                                              boolean pinFavs, int sortType,
                                              @NonNull String textFilter) {
    String textQuery = "%";
    // itty bitty FSM - Room needs better way
    if (TextUtils.isEmpty(textFilter)) {
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
      return mDB.clipDao().getAll(textQuery);
    } else {
      // filter by query text too
      textQuery = '%' + textFilter + '%';
      if (filterByFavs) {
        if (sortType == 1) {
          return mDB.clipDao().getFavsByText(textQuery);
        }
        return mDB.clipDao().getFavs(textQuery);
      }
      if (pinFavs) {
        if (sortType == 1) {
          return mDB.clipDao().getAllPinFavsByText(textQuery);
        }
        return mDB.clipDao().getAllPinFavs(textQuery);
      }
      if (sortType == 1) {
        return mDB.clipDao().getAllByText(textQuery);
      }
      return mDB.clipDao().getAll(textQuery);
    }
  }

  private void changeClips() {
    Log.logD(TAG, "clips source changed");
    clips.removeSource(clipsSource);
    clipsSource =
      getClips(filterByFavs, pinFavs, sortType, getClipTextFilterSync());
    clips.addSource(clipsSource, clips::setValue);
  }
}
