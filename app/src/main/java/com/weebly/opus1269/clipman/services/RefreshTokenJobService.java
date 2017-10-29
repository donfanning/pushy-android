/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.services;

import android.os.Handler;
import android.os.Looper;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backend.registration.model.EndpointRet;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.RegistrationClient;

/**
 * JobService to handle {@link FirebaseInstanceIdService} refreshToken()
 */
public class RefreshTokenJobService extends JobService {
  public static final String TAG = "RefreshTokenJobService";

  @Override
  public boolean onStartJob(JobParameters job) {
    Boolean ret = false;
    if (User.INST(this).isLoggedIn()) {
      ret = true;
      // Make sure we have looper
      final Handler handler = new Handler(Looper.getMainLooper());
      handler.post(new Runnable() {

        @Override
        public void run() {
          // Get updated InstanceID token.
          String refreshedToken = FirebaseInstanceId.getInstance().getToken();
          Log.logD(TAG, "Refreshed token: " + refreshedToken);
          Analytics.INST.instanceIdRefreshed();
          final EndpointRet ret = RegistrationClient.register(refreshedToken);
          if (!ret.getSuccess()) {
            Log.logE(TAG, ret.getReason(), false);
          }
        }
      });
    }
    return ret; // Answers the question: "Is there still work going on?"
  }

  @Override
  public boolean onStopJob(JobParameters job) {
    return false; // Answers the question: "Should this job be retried?"
  }
}
