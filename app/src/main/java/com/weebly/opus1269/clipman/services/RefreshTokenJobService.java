/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.services;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backend.registration.model.EndpointRet;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.msg.RegistrationClient;

/**
 * JobService to handle {@link FirebaseInstanceIdService} refreshToken()
 */
public class RefreshTokenJobService extends JobService {
  public static final String TAG = "RefreshTokenJobService";

  @Override
  public boolean onStartJob(JobParameters job) {
    Log.logD(TAG, "onStartJob");
    Boolean ret = false;
    if (Prefs.INST(this).isDeviceRegistered()) {
      ret = true;
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          String token = FirebaseInstanceId.getInstance().getToken();
          Log.logD(TAG, "Refreshed token: " + token);
          final EndpointRet ret =
            RegistrationClient.register(null, true);
          if (!ret.getSuccess()) {
            Log.logE(TAG, ret.getReason(), false);
          } else {
            Log.logD(TAG, "Reregister success");
            Analytics.INST.instanceIdRefreshed();
          }
        }
      });
      thread.start();
    }
    return ret; // Answers the question: "Is there still work going on?"
  }

  @Override
  public boolean onStopJob(JobParameters job) {
    return false; // Answers the question: "Should this job be retried?"
  }
}
