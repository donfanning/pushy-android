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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.firebase.iid.FirebaseInstanceId;
import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.User;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Base class for Google App Engine Endpoints */
abstract class Endpoint {

  /** Set to true to use local gae server */
  final static private boolean USE_LOCAL_SERVER = true;

  /** Global Application Context */
  final Context mContext;

  final String ERROR_UNKNOWN;

  final String ERROR_CREDENTIAL;

  final private String ERROR_ID_TOKEN;

  final private String ERROR_ACCOUNT;

  /** Log tag */
  final private String TAG = "Endpoint";

  /** Access id */
  final private String WEB_CLIENT_ID;

  /** Network timeout in millisecs */
  final private int TIMEOUT = 40000;

  final private String ERROR_NOT_SIGNED_IN;

  /** Global NetHttpTransport instance */
  private NetHttpTransport mNetHttpTransport = null;

  /** Global AndroidJacksonFactory instance */
  private AndroidJsonFactory mAndroidJsonFactory = null;

  Endpoint(@NonNull Context context) {
    mContext = context.getApplicationContext();

    ERROR_UNKNOWN = mContext.getString(R.string.err_unknown);
    ERROR_CREDENTIAL = mContext.getString(R.string.err_credential);
    ERROR_ACCOUNT = mContext.getString(R.string.err_account);
    ERROR_ID_TOKEN = mContext.getString(R.string.err_id_token);
    ERROR_NOT_SIGNED_IN = mContext.getString(R.string.err_not_signed_in);

    WEB_CLIENT_ID = mContext.getString(R.string.default_web_client_id);
  }

  /**
   * Determine if we are signed in
   * @return true if not signed in
   */
  boolean notSignedIn() {
    if (!User.INST(mContext).isLoggedIn()) {
      Log.logD(TAG, ERROR_NOT_SIGNED_IN);
      return true;
    }
    return false;
  }

  /**
   * Get InstanceId (regToken)
   * @return regToken
   */
  String getRegToken() throws IOException {
    String ret = FirebaseInstanceId.getInstance().getToken();
    if (ret == null) {
      throw new IOException("Failed to get registration token.");
    }
    return ret;
  }

  /**
   * Get shared NetHttpTransport
   * @return regToken
   */
  NetHttpTransport getNetHttpTransport() {
    if (mNetHttpTransport == null) {
      mNetHttpTransport = new NetHttpTransport();
    }
    return mNetHttpTransport;
  }

  /**
   * Get shared AndroidJsonFactory
   * @return regToken
   */
  AndroidJsonFactory getAndroidJsonFactory() {
    if (mAndroidJsonFactory == null) {
      mAndroidJsonFactory = new AndroidJsonFactory();
    }
    return mAndroidJsonFactory;
  }

  /**
   * Get an idToken for authorization
   * @return idToken
   */
  private String getIdToken() throws IOException {
    String idToken;

    // Get the IDToken that can be used securely on the backend for a short time
    final GoogleSignInOptions gso =
      new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(WEB_CLIENT_ID)
        .build();

    // Build a GoogleApiClient with access to the Google Sign-In API and the
    // options specified by gso.
    final GoogleApiClient googleApiClient =
      new GoogleApiClient.Builder(mContext)
        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
        .build();

    try {
      final ConnectionResult connRes =
        googleApiClient.blockingConnect(TIMEOUT, TimeUnit.MILLISECONDS);
      if (connRes.isSuccess()) {
        final GoogleSignInResult signInRes =
          Auth.GoogleSignInApi
            .silentSignIn(googleApiClient)
            .await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (signInRes.isSuccess()) {
          final GoogleSignInAccount acct =
            signInRes.getSignInAccount();
          if (acct != null) {
            idToken = acct.getIdToken();
          } else {
            throw new IOException(ERROR_ACCOUNT);
          }
        } else {
          final String msg = signInRes.getStatus().toString();
          throw new IOException(msg);
        }
      } else {
        final String msg = connRes.getErrorMessage() +
          "Code=" + connRes.getErrorCode();
        throw new IOException(msg);
      }
    } catch (Exception ex) {
      final String msg = ERROR_ID_TOKEN + ": " + ex.getLocalizedMessage();
      throw new IOException(msg);
    } finally {
      googleApiClient.disconnect();
    }

    return idToken;
  }

  /**
   * Get a {@link GoogleCredential} for authorized server call
   * @param idToken - authorization token for user
   * @return {@link GoogleCredential} for authorized server call
   */
  @Nullable
  GoogleCredential getCredential(String idToken) throws IOException {

    // get credential for a server call
    final GoogleCredential.Builder builder = new GoogleCredential.Builder();
    final GoogleCredential credential = builder
      .setTransport(getNetHttpTransport())
      .setJsonFactory(getAndroidJsonFactory())
      .build();

    final String token;
    if (TextUtils.isEmpty(idToken)) {
      token = getIdToken();
    } else {
      token = idToken;
    }

    if (TextUtils.isEmpty(token)) {
      return null;
    }

    credential.setAccessToken(token);

    return credential;
  }

  /**
   * Set setRootUrl and setGoogleClientRequestInitializer
   * for running with local server
   * @param builder JSON client
   */
  void setLocalServer(AbstractGoogleJsonClient.Builder builder) {
    if (USE_LOCAL_SERVER && BuildConfig.DEBUG) {
      // options for running against local devappserver
      // - 10.0.2.2 is localhost's IP address in Android emulator
      // - turn off compression when running against local devappserver
      builder.setRootUrl("http://10.0.2.2:8080/_ah/api/")
        .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer
          () {
          @Override
          public void initialize(AbstractGoogleClientRequest<?> request) {
            request.setDisableGZipContent(true);
          }
        });
    }
  }

  /**
   * Set timeout for authorized calls
   * @param requestInitializer initialize timeouts
   * @return HttpRequestInitializer
   */
  HttpRequestInitializer setHttpTimeout(
    final HttpRequestInitializer requestInitializer) {

    return new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest httpRequest) throws IOException {
        requestInitializer.initialize(httpRequest);
        httpRequest.setConnectTimeout(TIMEOUT);  // connect timeout millisecs
        httpRequest.setReadTimeout(TIMEOUT);  // read timeout millisecs
      }
    };
  }
}
