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
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;

import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.util.ArrayList;
import java.util.List;

/** ViewModel for MainActvity */
public class MainViewModel extends BaseRepoViewModel<MainRepo> implements
  SharedPreferences.OnSharedPreferenceChangeListener {
  /** Clips list */
  private final MediatorLiveData<List<ClipEntity>> clips;

  /** Clips Source */
  private LiveData<List<ClipEntity>> clipsSource;

  /** Sort with favorites first if true */
  public boolean pinFavs;

  /** Sort by date or text */
  public int sortType;

  /** Show only favorites if true */
  public boolean filterByFavs;

  /** Show only Clips with the given label if non-whitespace */
  public String labelFilter;

  public List<ClipEntity> undoItems;

  public MainViewModel(@NonNull Application app) {
    super(app, MainRepo.INST(app));

    pinFavs = Prefs.INST(app).isPinFav();

    filterByFavs = Prefs.INST(app).isFavFilter();

    labelFilter = Prefs.INST(app).getLabelFilter();
    Log.logD(TAG, "labelFilter: " + labelFilter);
    if (labelFilter == null) {
      labelFilter = "";
    }

    sortType = Prefs.INST(app).getSortType();

    undoItems = new ArrayList<>(0);

    clips = new MediatorLiveData<>();
    clips.setValue(null);
    clipsSource = mRepo.loadClips(filterByFavs, pinFavs, sortType);
    clips.addSource(clipsSource, clips::setValue);

    // listen for preference changes
    PreferenceManager
      .getDefaultSharedPreferences(app)
      .registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                        String key) {
    final Context context = getApplication();

    if (Prefs.INST(context).PREF_FAV_FILTER.equals(key)) {
      filterByFavs = Prefs.INST(context).isFavFilter();
    } else if (Prefs.INST(context).PREF_PIN_FAV.equals(key)) {
      pinFavs = Prefs.INST(context).isPinFav();
    } else if (Prefs.INST(context).PREF_SORT_TYPE.equals(key)) {
      sortType = Prefs.INST(context).getSortType();
    }
    Log.logD(TAG, "source changed");
    clips.removeSource(clipsSource);
    clipsSource = mRepo.loadClips(filterByFavs, pinFavs, sortType);
    clips.addSource(clipsSource, clips::setValue);
  }

  public LiveData<List<ClipEntity>> loadClips() {
    return clips;
  }
}
