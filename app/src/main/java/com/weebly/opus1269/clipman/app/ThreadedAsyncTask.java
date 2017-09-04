/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.app;

import android.os.AsyncTask;

/**
 * Run AsynTask in parallel
 * @see <a href="http://stackoverflow.com/a/19060929/4468645">Stack Overflow</a>
 */
public abstract class ThreadedAsyncTask<Params, Progress, Result> extends
  AsyncTask<Params, Progress, Result> {

  /**
   * Call this instead of execute() for parallel execution
   * @param params - passed in to doInBackground
   * @return This instance of AsyncTask
   */
  @SafeVarargs
  public final AsyncTask<Params, Progress, Result> executeMe(Params... params) {
    return super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
  }
}
