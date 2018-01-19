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
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.db.DeviceDB;
import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.model.BackupFile;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.msg.MessagingClient;

import java.util.ArrayList;
import java.util.List;

/** Singleton - Repository for {@link Device} objects */
public class BackupRepo implements
  SharedPreferences.OnSharedPreferenceChangeListener {
  @SuppressLint("StaticFieldLeak")
  private static BackupRepo sInstance;

  /** Application */
  private final Application mApp;

  ///** Database */
  //private final BackupDB mDB;

  /** Info message - not saved in database */
  private final MutableLiveData<String> infoMessage;

  /** Device List */
  private final MutableLiveData<List<BackupFile>> backupList;

  private BackupRepo(final Application app) {
    mApp = app;
    //mDB = DeviceDB.INST(app);

    infoMessage = new MutableLiveData<>();
    backupList = new MutableLiveData<>();
    backupList.setValue(new ArrayList<>());

  }

  public static BackupRepo INST(final Application app) {
    if (sInstance == null) {
      synchronized (BackupRepo.class) {
        if (sInstance == null) {
          sInstance = new BackupRepo(app);
        }
      }
    }
    return sInstance;
  }

  @Override
  public void
  onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    final String keyPush = mApp.getString(R.string.key_pref_push_msg);
    final String keyReceive = mApp.getString(R.string.key_pref_receive_msg);

    if (key.equals(keyPush)) {
      final String msg = Prefs.INST(mApp).isPushClipboard() ?
        "" : mApp.getString(R.string.err_no_push_to_devices);
      postInfoMessage(msg);
    } else if (key.equals(keyReceive)) {
      final String msg = Prefs.INST(mApp).isAllowReceive() ?
        "" : mApp.getString(R.string.err_no_receive_from_devices);
      postInfoMessage(msg);
    }
  }

  public LiveData<String> getInfoMessage() {
    return infoMessage;
  }

  private void setInfoMessage(String msg) {
    infoMessage.setValue(msg);
  }

  private void postInfoMessage(String msg) {
    infoMessage.postValue(msg);
  }

  public LiveData<List<BackupFile>> getBackupList() {
    return backupList;
  }

}
