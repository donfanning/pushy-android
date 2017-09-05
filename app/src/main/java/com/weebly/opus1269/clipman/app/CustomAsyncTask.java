
/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.app;

import android.app.Activity;

/**
 * This class is part of a solution to the problem of screen
 * orientation/Activity destruction during lengthy Async tasks.
 * {@see https://goo.gl/vsNa1h}
 */

public abstract class CustomAsyncTask<TParams, TProgress, TResult>
  extends ThreadedAsyncTask<TParams, TProgress, TResult> {
  protected static final String NO_ACTIVITY =
    "AsyncTask finished while no Activity was attached.";

  private final App mApp;
  protected Activity mActivity;

  public CustomAsyncTask(Activity activity) {
    mActivity = activity;
    mApp = (App) mActivity.getApplication();
  }

  public void setActivity(Activity activity) {
    mActivity = activity;
  }

  @Override
  protected void onPreExecute() {
    mApp.addTask(mActivity, this);
  }

  @Override
  protected void onPostExecute(TResult result) {
    mApp.removeTask(this);
  }

  @Override
  protected void onCancelled() {
    mApp.removeTask(this);
  }
}
