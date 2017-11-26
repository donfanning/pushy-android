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

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.firebase.iid.FirebaseInstanceId;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.CustomAsyncTask;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backend.registration.Registration;
import com.weebly.opus1269.clipman.backend.registration.model.EndpointRet;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Prefs;

import java.io.IOException;

/**
 * This helper class is the interface to our gae Registration Endpoint
 */
public class RegistrationClient extends Endpoint {
  private static final String TAG = "RegistrationClient";

  private static final String ERROR_REGISTER =
    App.getContext().getString(R.string.err_register);
  private static final String ERROR_UNREGISTER =
    App.getContext().getString(R.string.err_unregister);
  private static final String ERROR_INVALID_REGID =
    App.getContext().getString(R.string.err_invalid_regid);

  private RegistrationClient() {
  }

  /**
   * Register with server - Blocks
   * @param idToken - authorization token or null
   * @param refresh - true is refreshing exisitng token
   * @return getSuccess() false on error
   */
  public static EndpointRet register(String idToken, Boolean refresh) {
    final Context context = App.getContext();
    EndpointRet ret = new EndpointRet();
    ret.setSuccess(false);
    ret.setReason(Msg.ERROR_UNKNOWN);

    if (!refresh && notSignedIn()) {
      Log.logD(TAG, "Not signed in.");
      ret.setSuccess(true);
      return ret;
    } else if (!refresh && Prefs.INST(context).isDeviceRegistered()) {
      Log.logD(TAG, "Already registered.");
      ret.setSuccess(true);
      return ret;
    } else if (!Prefs.INST(context).isAllowReceive()) {
      Log.logD(TAG, "User doesn't want to receive messasges.");
      ret.setSuccess(true);
      return ret;
    }

    boolean isRegistered = false;
    try {
      final String regToken = getRegToken();
      if (TextUtils.isEmpty(regToken)) {
        ret.setReason(Log.logE(TAG, ERROR_INVALID_REGID, ERROR_REGISTER));
        return ret;
      }

      final GoogleCredential credential = getCredential(idToken);
      if (credential == null) {
        ret.setReason(Log.logE(TAG, Msg.ERROR_CREDENTIAL, ERROR_REGISTER));
        return ret;
      }

      // call server
      final Registration regService = getRegistrationService(credential);
      ret = regService.register(regToken).execute();
      if (ret.getSuccess()) {
        isRegistered = true;
        Analytics.INST.registered();
      } else {
        ret.setReason(Log.logE(TAG, ret.getReason(), ERROR_REGISTER));
      }
    } catch (final IOException ex) {
      ret.setReason(Log.logEx(TAG, ex.getLocalizedMessage(), ex,
        ERROR_REGISTER));
    } finally {
      Prefs.INST(context).setDeviceRegistered(isRegistered);
    }

    return ret;
  }

  /**
   * Unregister with server - Blocks
   * @return getSuccess() false on error
   */
  private static EndpointRet unregister() {
    final Context context = App.getContext();
    EndpointRet ret = new EndpointRet();
    ret.setSuccess(false);
    ret.setReason(Msg.ERROR_UNKNOWN);

    if (notSignedIn()) {
      ret.setSuccess(true);
      return ret;
    } else if (!Prefs.INST(context).isDeviceRegistered()) {
      Log.logE(TAG, Msg.ERROR_NOT_REGISTERED, ERROR_UNREGISTER);
      ret.setSuccess(true);
      return ret;
    }

    boolean isRegistered = true;
    try {
      final String regToken = getRegToken();
      if (TextUtils.isEmpty(regToken)) {
        ret.setReason(Log.logE(TAG, ERROR_INVALID_REGID, ERROR_UNREGISTER));
        return ret;
      }

      final GoogleCredential credential = getCredential(null);
      if (credential == null) {
        ret.setReason(Log.logE(TAG, Msg.ERROR_CREDENTIAL, ERROR_UNREGISTER));
        return ret;
      }

      // call server
      final Registration regService = getRegistrationService(credential);
      ret = regService.unregister(regToken).execute();
      if (ret.getSuccess()) {
        Analytics.INST.unregistered();
        isRegistered = false;
      } else {
        ret.setReason(Log.logE(TAG, ret.getReason(), ERROR_UNREGISTER));
      }
    } catch (final IOException ex) {
      ret.setReason(Log.logEx(TAG, ex.getLocalizedMessage(), ex,
        ERROR_UNREGISTER));
    } finally {
      Prefs.INST(context).setDeviceRegistered(isRegistered);
    }

    return ret;
  }

  /**
   * Get an authorized connection to the RegistrationEndpoint
   * @param credential - authorization for current user
   * @return Connection to RegistrationEndpoint on server
   */
  private static Registration
  getRegistrationService(GoogleCredential credential) {
    final Registration.Builder builder =
      new Registration.Builder(new NetHttpTransport(),
        new AndroidJsonFactory(), credential);

    builder.setApplicationName(AppUtils.getApplicationName());

    // for development purposes
    setLocalServer(builder);

    return builder.build();
  }

  /** AsyncTask to register our with the server */
  public static class RegisterAsyncTask extends
    CustomAsyncTask<Void, Void, String> {

    private final String mIdToken;

    public RegisterAsyncTask(Activity activity, String idToken) {
      super(activity);
      mIdToken = idToken;
    }

    @Override
    protected String doInBackground(Void... params) {
      String error = "";
      // register device with the server - blocks
      EndpointRet ret = RegistrationClient.register(mIdToken, false);
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
          MessagingClient.sendDeviceAdded();
          // notifiy listeners
          Devices.INST(mActivity).notifyMyDeviceRegistered();
        }
      } else {
        Log.logE(TAG, NO_ACTIVITY, false);
      }
    }
  }

  /** AsyncTask to unregister from server */
  public static class UnregisterAsyncTask extends
    CustomAsyncTask<Void, Void, String> {

    public UnregisterAsyncTask(Activity activity) {
      super(activity);
    }

    @Override
    protected String doInBackground(Void... params) {
      String error = "";
      // unregister with the server - blocks
      EndpointRet ret = RegistrationClient.unregister();
      if (!ret.getSuccess()) {
        Prefs.INST(App.getContext()).setDeviceRegistered(false);
        try {
          // delete in case user logs into different account - blocks
          FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (IOException ex) {
          Log.logEx(TAG, "", ex, false);
        }
        error = ret.getReason();
        Log.logE(TAG, error, false);
      }
      return error;
    }

    @Override
    protected void onPostExecute(String error) {
      // must call
      super.onPostExecute(error);

      // SignInActivity will be notified that it can now sign-out
      Devices.INST(App.getContext()).notifyMyDeviceUnregistered();
    }
  }
}
