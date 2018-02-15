/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.services;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.ClipboardHelper;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.entity.ClipItem;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.repos.MainRepo;

/**
 * An app private {@link Service} to listen for changes to the clipboard,
 * persist them to storage and push them to registered FCM devices
 */
public class ClipboardWatcherService extends Service implements
  ClipboardManager.OnPrimaryClipChangedListener {
  private static final String TAG = "ClipboardWatcherService";

  /**
   * {@link Intent} extra to indicate if service should process clipboard on
   * start
   */
  private static final String EXTRA_NO_PROCESS_ON_START = "noProcessOnStart";

  /** The fastest we will process identical local copies {@value} */
  private static final long MIN_TIME_MILLIS = 200;

  /** The last text we read */
  private String mLastText;

  /** The last time we read */
  private long mLastTime;

  /** ye olde ClipboardManager */
  private ClipboardManager mClipboard = null;

  /**
   * Start ourselves
   * @param ctxt             a Context
   * @param noProcessOnStart true if we should not process clipboard on start
   */
  @TargetApi(26)
  public static void startService(Context ctxt, Boolean noProcessOnStart) {
    if (Prefs.INST(ctxt).isMonitorClipboard()
      && !AppUtils.isMyServiceRunning(ctxt, ClipboardWatcherService.class)) {
      // only start if the user has allowed it and we are not running
      final Intent intent = new Intent(ctxt, ClipboardWatcherService.class);
      intent.putExtra(EXTRA_NO_PROCESS_ON_START, noProcessOnStart);
      if (AppUtils.isOreoOrLater()) {
        ctxt.startForegroundService(intent);
      } else {
        ctxt.startService(intent);
      }
    }
  }

  @Override
  public void onCreate() {
    if (AppUtils.isOreoOrLater()) {
      Notifications.INST(getApplicationContext()).startAndShow(this);
    }

    mClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    if (mClipboard != null) {
      mClipboard.addPrimaryClipChangedListener(this);
    } else {
      Log.logE(this, TAG, "Failed to get ClipboardManager");
    }

    mLastText = "";
    mLastTime = System.currentTimeMillis();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    mLastText = "";
    mLastTime = System.currentTimeMillis();

    if (intent != null) {
      final boolean noProcessOnStart =
        intent.getBooleanExtra(EXTRA_NO_PROCESS_ON_START, false);
      if (!noProcessOnStart) {
        processClipboard(true);
      }
    } else {
      processClipboard(true);
    }

    Log.logD(TAG, "started");

    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mClipboard != null) {
      mClipboard.removePrimaryClipChangedListener(this);
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException(
      "Unimplemented onBind method in: " + TAG);
  }

  @Override
  public void onPrimaryClipChanged() {
    processClipboard(false);
  }

  /**
   * Read the clipboard and process the result based on what is there.
   * @param onNewOnly if true, only update database if text is new
   */
  private void processClipboard(boolean onNewOnly) {
    if (mClipboard == null) {
      return;
    }
    final ClipItem clip = ClipboardHelper.getFromClipboard(this, mClipboard);
    final long now = System.currentTimeMillis();
    final long deltaTime = now - mLastTime;
    mLastTime = now;

    if (ClipItem.isWhitespace(clip) || clip.getRemote()) {
      // ignore empty or remote clips - remotes were saved by FCM listener
      mLastText = "";
      return;
    }

    if (mLastText.equals(clip.getText())) {
      if (deltaTime > MIN_TIME_MILLIS) {
        // only handle identical local copies this fast
        // some apps (at least Chrome) write to clipboard twice.
        saveAndSend(clip, onNewOnly);
      }
    } else {
      // normal situation, fire away
      saveAndSend(clip, onNewOnly);
    }
    mLastText = clip.getText();
  }

  /**
   * Optionally save to database and send to remote devices
   * @param clip      item
   * @param onNewOnly if true, only save if the text doesn't exist
   */
  private void saveAndSend(ClipItem clip, boolean onNewOnly) {
    MainRepo.INST(App.INST()).addClipAndSend(clip, onNewOnly);
  }
}
