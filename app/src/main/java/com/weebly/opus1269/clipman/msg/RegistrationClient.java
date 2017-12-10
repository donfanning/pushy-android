/*
 *
 * Copyright 2016 Michael A Updike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.weebly.opus1269.clipman.msg;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.firebase.iid.FirebaseInstanceId;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.CustomAsyncTask;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backend.registration.Registration;
import com.weebly.opus1269.clipman.backend.registration.model.EndpointRet;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Prefs;

import java.io.IOException;

/** Singleton that is the interface to our gae Registration Endpoint */
public class RegistrationClient extends Endpoint {

  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static RegistrationClient sInstance;

  private final String TAG = "RegistrationClient";
  private final String ERROR_REGISTER;
  private final String ERROR_UNREGISTER;
  private final String ERROR_INVALID_REGID;
  private final String ERROR_NOT_REGISTERED;

  private RegistrationClient(@NonNull Context context) {
    super(context);

    ERROR_REGISTER = context.getString(R.string.err_register);
    ERROR_UNREGISTER = context.getString(R.string.err_unregister);
    ERROR_INVALID_REGID = context.getString(R.string.err_invalid_regid);
    ERROR_NOT_REGISTERED = context.getString(R.string.err_not_registered);
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static RegistrationClient INST(@NonNull Context context) {
    synchronized (RegistrationClient.class) {
      if (sInstance == null) {
        sInstance = new RegistrationClient(context);
      }
      return sInstance;
    }
  }

  /**
   * Register with server - Blocks
   * @param idToken - authorization token or null
   * @param refresh - true is refreshing exisitng token
   * @return getSuccess() false on error
   */
  public EndpointRet register(String idToken, Boolean refresh) {
    EndpointRet ret = new EndpointRet();
    ret.setSuccess(false);
    ret.setReason(ERROR_UNKNOWN);

    if (!refresh && notSignedIn()) {
      Log.logD(TAG, "Not signed in.");
      ret.setSuccess(true);
      return ret;
    } else if (!refresh && Prefs.INST(mContext).isDeviceRegistered()) {
      Log.logD(TAG, "Already registered.");
      ret.setSuccess(true);
      return ret;
    } else if (!Prefs.INST(mContext).isAllowReceive()) {
      Log.logD(TAG, "User doesn't want to receive messasges.");
      ret.setSuccess(true);
      return ret;
    }

    boolean isRegistered = false;
    try {
      final String regToken = getRegToken();
      if (TextUtils.isEmpty(regToken)) {
        ret.setReason(
          Log.logE(mContext, TAG, ERROR_INVALID_REGID, ERROR_REGISTER));
        return ret;
      }

      final GoogleCredential credential = getCredential(idToken);
      if (credential == null) {
        ret.setReason(
          Log.logE(mContext, TAG, ERROR_CREDENTIAL, ERROR_REGISTER));
        return ret;
      }

      // call server
      final Registration regService = getRegistrationService(credential);
      ret = regService.register(regToken).execute();
      if (ret.getSuccess()) {
        isRegistered = true;
        Analytics.INST(mContext).registered();
      } else {
        ret.setReason(Log.logE(mContext, TAG, ret.getReason(), ERROR_REGISTER));
      }
    } catch (final Exception ex) {
      ret.setReason(Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex,
        ERROR_REGISTER));
    } finally {
      Prefs.INST(mContext).setDeviceRegistered(isRegistered);
    }

    return ret;
  }

  /**
   * Unregister with server - Blocks
   * @return getSuccess() false on error
   */
  private EndpointRet unregister() {
    EndpointRet ret = new EndpointRet();
    ret.setSuccess(false);
    ret.setReason(ERROR_UNKNOWN);

    if (notSignedIn()) {
      ret.setSuccess(true);
      return ret;
    } else if (!Prefs.INST(mContext).isDeviceRegistered()) {
      Log.logE(mContext, TAG, ERROR_NOT_REGISTERED, ERROR_UNREGISTER);
      ret.setSuccess(true);
      return ret;
    }

    boolean isRegistered = true;
    try {
      final String regToken = getRegToken();
      if (TextUtils.isEmpty(regToken)) {
        ret.setReason(Log.logE(mContext, TAG, ERROR_INVALID_REGID,
          ERROR_UNREGISTER));
        return ret;
      }

      final GoogleCredential credential = getCredential(null);
      if (credential == null) {
        ret.setReason(
          Log.logE(mContext, TAG, ERROR_CREDENTIAL, ERROR_UNREGISTER));
        return ret;
      }

      // call server
      final Registration regService = getRegistrationService(credential);
      ret = regService.unregister(regToken).execute();
      if (ret.getSuccess()) {
        Analytics.INST(mContext).unregistered();
        isRegistered = false;
      } else {
        ret.setReason(
          Log.logE(mContext, TAG, ret.getReason(), ERROR_UNREGISTER));
      }
    } catch (final Exception ex) {
      ret.setReason(Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex,
        ERROR_UNREGISTER));
    } finally {
      Prefs.INST(mContext).setDeviceRegistered(isRegistered);
    }

    return ret;
  }

  /**
   * Get an authorized connection to the RegistrationEndpoint
   * @param credential - authorization for current user
   * @return Connection to RegistrationEndpoint on server
   */
  @NonNull
  private Registration getRegistrationService(GoogleCredential credential) {
    final Registration.Builder builder =
      new Registration.Builder(getNetHttpTransport(),
        getAndroidJsonFactory(), setHttpTimeout(credential));

    builder.setApplicationName(AppUtils.getAppName(mContext));

    // for development purposes
    setLocalServer(builder);

    return builder.build();
  }

  /** AsyncTask to register our with the server */
  public static class RegisterAsyncTask extends
    CustomAsyncTask<Void, Void, String> {

    // OK, if mAppContext is the global Application context
    @SuppressLint("StaticFieldLeak")
    private final Context mAppContext;

    private final String mIdToken;

    public RegisterAsyncTask(Context appContext, Activity activity,
                             String idToken) {
      super(activity);
      mAppContext = appContext;
      mIdToken = idToken;
    }

    @Override
    protected String doInBackground(Void... params) {
      String error = "";
      // register device with the server - blocks
      EndpointRet ret =
        RegistrationClient.INST(mAppContext).register(mIdToken, false);
      if (!ret.getSuccess()) {
        error = ret.getReason();
      }
      return error;
    }

    @Override
    protected void onPostExecute(String error) {
      // must call
      super.onPostExecute(error);

      if (mActivity != null) {
        if (!TextUtils.isEmpty(error)) {
          // notifiy listeners
          Devices.INST(mActivity).notifyMyDeviceRegisterError(error);
        } else {
          // let others know we are here
          MessagingClient.INST(mAppContext).sendDeviceAdded();
          // notifiy listeners
          Devices.INST(mActivity).notifyMyDeviceRegistered();
        }
      } else {
        Log.logE(mAppContext, "RegisterAsyncTask", NO_ACTIVITY, false);
      }
    }
  }

  /** AsyncTask to unregister from server */
  public static class UnregisterAsyncTask extends
    CustomAsyncTask<Void, Void, String> {
    private static final String TAG = "UnregisterAsyncTask";

    // OK, if mAppContext is the global Application context
    @SuppressLint("StaticFieldLeak")
    private final Context mAppContext;

    public UnregisterAsyncTask(Context appContext, Activity activity) {
      super(activity);
      mAppContext = appContext;
    }

    @Override
    protected String doInBackground(Void... params) {
      String error = "";
      // unregister with the server - blocks
      EndpointRet ret = RegistrationClient.INST(mAppContext).unregister();
      if (!ret.getSuccess()) {
        Prefs.INST(mAppContext).setDeviceRegistered(false);
        try {
          // delete in case user logs into different account - blocks
          FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (IOException ex) {
          Log.logEx(mAppContext, TAG, "", ex, false);
        }
        error = ret.getReason();
        Log.logE(mAppContext, TAG, error, false);
      }
      return error;
    }

    @Override
    protected void onPostExecute(String error) {
      // must call
      super.onPostExecute(error);

      // SignInActivity will be notified that it can now sign-out
      Devices.INST(mAppContext).notifyMyDeviceUnregistered();
    }
  }
}
