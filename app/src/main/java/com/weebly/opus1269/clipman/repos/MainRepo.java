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
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.ClipLabelJoin;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.model.Prefs;

import java.util.List;

/** Singleton - Repository for the {@link MainDB} */
public class MainRepo extends BaseRepo implements
  SharedPreferences.OnSharedPreferenceChangeListener {
  @SuppressLint("StaticFieldLeak")
  private static MainRepo sInstance;

  /** Database */
  private final MainDB mDB;

  /** Clips list */
  @NonNull
  private final MediatorLiveData<List<Clip>> clips;

  /** Label list */
  @NonNull
  private final MediatorLiveData<List<Label>> labels;

  /** Selected clip */
  @NonNull
  private final MediatorLiveData<Clip> selClip;

  /** Selected clip's labels */
  @NonNull
  private final MediatorLiveData<List<Label>> selLabels;

  /** Show only Clips with the given label */
  @NonNull
  private final MutableLiveData<Label> filterLabel;

  /** Text to filter clips on */
  @NonNull
  private final MutableLiveData<String> clipTextFilter;

  /** Clips Source */
  @Nullable
  private LiveData<List<Clip>> clipsSource;

  /** Selected clip Source */
  @Nullable
  private LiveData<Clip> selClipSource;

  /** Selected clips Lables Source */
  @Nullable
  private LiveData<List<Label>> selLabelsSource;

  /** Sort with favorites first if true */
  private boolean pinFavs;

  /** Show only favorites if true */
  private boolean filterByFavs;

  /** Sort by date or text */
  private int sortType;

  private MainRepo(final Application app) {
    super(app);

    mDB = MainDB.INST(app);

    pinFavs = Prefs.INST(app).isPinFav();
    filterByFavs = Prefs.INST(app).isFavFilter();
    sortType = Prefs.INST(app).getSortType();

    clipTextFilter = new MutableLiveData<>();
    clipTextFilter.postValue("");

    filterLabel = new MutableLiveData<>();
    filterLabel.observeForever(label -> changeClipsSource());

    selClip = new MediatorLiveData<>();
    selClip.postValue(null);
    selClipSource = null;

    selLabels = new MediatorLiveData<>();
    selLabels.postValue(null);
    selLabelsSource = null;

    labels = new MediatorLiveData<>();
    labels.postValue(null);
    labels.addSource(mDB.labelDao().getAll(), labels -> {
      if (mDB.getDatabaseCreated().getValue() != null) {
        this.labels.postValue(labels);
        if ((labels != null) && !labels.contains(filterLabel.getValue())) {
          // reset filterlabel if the Label is deleted
          filterLabel.postValue(null);
        }
      }
    });

    clips = new MediatorLiveData<>();
    clips.postValue(null);
    clipsSource = null;

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

    //noinspection IfCanBeSwitch
    if (Prefs.INST(context).PREF_FAV_FILTER.equals(key)) {
      filterByFavs = Prefs.INST(context).isFavFilter();
      final Clip clip = getSelClip().getValue();
      if (filterByFavs && (clip != null) && !clip.getFav()) {
        // unselect if we will be filtered out
        setSelClip(null);
      }
    } else if (Prefs.INST(context).PREF_PIN_FAV.equals(key)) {
      pinFavs = Prefs.INST(context).isPinFav();
    } else if (Prefs.INST(context).PREF_SORT_TYPE.equals(key)) {
      sortType = Prefs.INST(context).getSortType();
    } else {
      // not ours to handle
      return;
    }

    changeClipsSource();
  }

  @NonNull
  public LiveData<List<Clip>> getClips() {
    return clips;
  }

  @NonNull
  public LiveData<List<Label>> getLabels() {
    return labels;
  }

  @NonNull
  public LiveData<Clip> getSelClip() {
    return selClip;
  }

  public void setSelClip(@Nullable Clip clip) {
    if (selClipSource != null) {
      selClip.removeSource(selClipSource);
    }
    if (Clip.isWhitespace(clip)) {
      // no clip
      selClipSource = null;
      selClip.setValue(null);
      setSelLabels(null);
    } else {
      selClipSource = getClip(clip.getId());
      selClip.addSource(selClipSource, selClip -> {
        setSelLabels(selClip);
        this.selClip.setValue(selClip);
      });
    }
  }

  @NonNull
  public LiveData<List<Label>> getSelLabels() {
    return selLabels;
  }

  private void setSelLabels(@Nullable Clip clip) {
    if (selLabelsSource != null) {
      selLabels.removeSource(selLabelsSource);
    }
    if (Clip.isWhitespace(clip)) {
      // no clip
      selLabelsSource = null;
      selLabels.setValue(null);
    } else {
      selLabelsSource = mDB.clipLabelJoinDao().getLabelsForClip(clip.getId());
      selLabels.addSource(selLabelsSource, this.selLabels::setValue);
    }
  }

  @NonNull
  public LiveData<Clip> getClip(final long id) {
    return mDB.clipDao().get(id);
  }

  @NonNull
  public LiveData<Label> getLabel(final long id) {
    return mDB.labelDao().get(id);
  }

  @NonNull
  public LiveData<List<Label>> getLabelsForClip(@NonNull Clip clip) {
    return mDB.clipLabelJoinDao().getLabelsForClip(clip.getId());
  }

  @NonNull
  public LiveData<Label> getFilterLabel() {
    return filterLabel;
  }

  public void setFilterLabel(@Nullable Label label) {
    filterLabel.setValue(label);
  }

  @NonNull
  public LiveData<String> getClipTextFilter() {
    return clipTextFilter;
  }

  public void setClipTextFilter(@NonNull String s) {
    if (!s.equals(clipTextFilter.getValue())) {
      clipTextFilter.setValue(s);
      changeClipsSource();
    }
  }

  @NonNull
  public String getClipTextFilterSync() {
    final String s = clipTextFilter.getValue();
    return (s == null) ? "" : s;
  }

  /**
   * Insert or replace clip but preserve fav state if it is true
   * @param clip Clip
   */
  public void addClip(@NonNull Clip clip) {
    App.getExecutors().diskIO().execute(() -> addClipSync(clip));
  }

  /**
   * Insert or replace clip but preserve fav state if it is true
   * @param clip Clip
   */
  public long addClipSync(@NonNull Clip clip) {
    final Clip oldClip = mDB.clipDao().getSync(clip.getText());
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
  public void addClipIfNew(@NonNull Clip clip) {
    addClipIfNew(clip, false);
  }

  /**
   * Insert a clip only if the text does not exist
   * @param clip   Clip
   * @param silent if true, no messages
   */
  public void addClipIfNew(@NonNull Clip clip, boolean silent) {
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
  public void addClipAndSend(Clip clip, boolean onNewOnly) {
    App.getExecutors().diskIO().execute(() -> {
      final Context context = mApp;
      long id = -1L;
      int nRows = 0;
      if (onNewOnly) {
        id = mDB.clipDao().insertIfNew(clip);
      } else {
        final Clip existingClip = mDB.clipDao().getSync(clip.getText());
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

  public void updateClip(@NonNull Clip clip) {
    App.getExecutors().diskIO().execute(() -> {
      final int nRows = mDB.clipDao().update(clip);
      if (nRows == 0) {
        postErrorMsg(new ErrorMsg(mApp.getString(R.string.repo_clip_exists)));
      } else {
        postInfoMessage(mApp.getString(R.string.repo_clip_updated));
      }
    });
  }

  public void updateClipFav(@NonNull Clip clip) {
    App.getExecutors().diskIO()
      .execute(() -> mDB.clipDao().updateFav(clip.getText(), clip.getFav()));
  }

  public void removeClipSync(@NonNull Clip clip) {
    final List<Label> labels = getLabelsForClipSync(clip);
    clip.setLabels(labels);
    mDB.clipDao().delete(clip);
  }

  public List<Clip> removeAllClipsSync(boolean includeFavs) {
    final List<Clip> clips;
    if (includeFavs) {
      clips = mDB.clipDao().getAllSync();
      setLabelsForClipsSync(clips);
      mDB.clipDao().deleteAll();
    } else {
      clips = mDB.clipDao().getNonFavsSync();
      setLabelsForClipsSync(clips);
      mDB.clipDao().deleteAllNonFavs();
    }
    return clips;
  }

  public void addLabelIfNew(@NonNull Label label) {
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

  public void removeLabel(@NonNull Label label) {
    App.getExecutors().diskIO().execute(() -> mDB.labelDao().delete(label));
  }

  public void addLabelForClip(@NonNull Clip clip, @NonNull Label label) {
    App.getExecutors().diskIO().execute(() -> mDB.clipLabelJoinDao()
      .insert(new ClipLabelJoin(clip.getId(), label.getId())));
  }

  public void removeLabelForClip(@NonNull Clip clip, @NonNull Label label) {
    App.getExecutors().diskIO().execute(() -> mDB.clipLabelJoinDao()
      .delete(new ClipLabelJoin(clip.getId(), label.getId())));
  }

  public void addClipsAndLabels(@NonNull List<Clip> clips) {
    final Runnable transaction = () -> mDB.runInTransaction(() -> {
      for (Clip clip : clips) {
        mDB.clipDao().insert(clip);
        final List<Label> labels = clip.getLabels();
        for (Label label : labels) {
          ClipLabelJoin join = new ClipLabelJoin(clip.getId(), label.getId());
          mDB.clipLabelJoinDao().insert(join);
        }
      }
    });

    App.getExecutors().diskIO().execute(transaction);
  }

  /**
   * Does a clip with the given text exist
   * @param clip Clip
   */
  private boolean exists(@NonNull Clip clip) {
    return mDB.clipDao().getSync(clip.getText()) != null;
  }

  /**
   * Get the List of Clips based on the various filter/sort options
   * @param filterLabel Label to filter on
   * @return List of Clips
   */
  private LiveData<List<Clip>> getClipsSource(@Nullable Label filterLabel) {
    long id = (filterLabel == null) ? -1L : filterLabel.getId();
    final boolean haslabelId = (id != -1L);
    final String textFilter = getClipTextFilterSync();

    // itty bitty FSM - Room needs better way
    if (TextUtils.isEmpty(textFilter)) {
      if (filterByFavs) {
        if (sortType == 1) {
          return haslabelId ?
            mDB.clipLabelJoinDao().getClipsForLabelFavsByText(id) :
            mDB.clipDao().getFavsByText();
        }
        return haslabelId ?
          mDB.clipLabelJoinDao().getClipsForLabelFavs(id) :
          mDB.clipDao().getFavs();
      }
      if (pinFavs) {
        if (sortType == 1) {
          return haslabelId ?
            mDB.clipLabelJoinDao().getClipsForLabelPinFavsByText(id) :
            mDB.clipDao().getAllPinFavsByText();
        }
        return haslabelId ?
          mDB.clipLabelJoinDao().getClipsForLabelPinFavs(id) :
          mDB.clipDao().getAllPinFavs();
      }
      if (sortType == 1) {
        return haslabelId ?
          mDB.clipLabelJoinDao().getClipsForLabelByText(id) :
          mDB.clipDao().getAllByText();
      }
      return haslabelId ?
        mDB.clipLabelJoinDao().getClipsForLabel(id) :
        mDB.clipDao().getAll();
    } else {
      // filter by query text too
      final String query = '%' + textFilter + '%';
      if (filterByFavs) {
        if (sortType == 1) {
          return haslabelId ?
            mDB.clipLabelJoinDao().getClipsForLabelFavsByText(id, query) :
            mDB.clipDao().getFavsByText(query);
        }
        return haslabelId ?
          mDB.clipLabelJoinDao().getClipsForLabelFavs(id, query) :
          mDB.clipDao().getFavs(query);
      }
      if (pinFavs) {
        if (sortType == 1) {
          return haslabelId ?
            mDB.clipLabelJoinDao().getClipsForLabelPinFavsByText(id, query) :
            mDB.clipDao().getAllPinFavsByText(query);
        }
        return haslabelId ?
          mDB.clipLabelJoinDao().getClipsForLabelPinFavs(id, query) :
          mDB.clipDao().getAllPinFavs(query);
      }
      if (sortType == 1) {
        return haslabelId ?
          mDB.clipLabelJoinDao().getClipsForLabelByText(id, query) :
          mDB.clipDao().getAllByText(query);
      }
      return haslabelId ?
        mDB.clipLabelJoinDao().getClipsForLabel(id, query) :
        mDB.clipDao().getAll(query);
    }
  }

  @NonNull
  private List<Label> getLabelsForClipSync(@NonNull Clip clip) {
    return mDB.clipLabelJoinDao().getLabelsForClipSync(clip.getId());
  }

  private void setLabelsForClipsSync(@NonNull List<Clip> clips) {
    for (final Clip clip : clips) {
      final List<Label> labels = getLabelsForClipSync(clip);
      clip.setLabels(labels);
    }
  }

  private void changeClipsSource() {
    if (clipsSource != null) {
      clips.removeSource(clipsSource);
    }
    clipsSource = getClipsSource(filterLabel.getValue());
    clips.addSource(clipsSource, clips::setValue);
  }
}
