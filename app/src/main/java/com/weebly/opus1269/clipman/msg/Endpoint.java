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
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.firebase.iid.FirebaseInstanceId;
import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.User;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Google App Engine Endpoints
 */
abstract class Endpoint {
  private static final String TAG = "Endpoint";

  /** Set to true to use local gae server - {@value} */
  private static final boolean USE_LOCAL_SERVER = false;

  /** Network timeout in seconds - {@value} */
  private static final int TIMEOUT = 60;

  /** Global NetHttpTransport instance */
  private static  NetHttpTransport sNetHttpTransport = null;

  /** Global AndroidJacksonFactory instance */
  private static AndroidJsonFactory sAndroidJsonFactory = null;

  /**
   * Determine if we are signed in
   * @return true if not signed in
   */
  static boolean notSignedIn() {
    if (!User.INST(App.getContext()).isLoggedIn()) {
      Log.logD(TAG, Msg.ERROR_NOT_SIGNED_IN);
      return true;
    }
    return false;
  }

  /**
   * Get InstanceId (regToken)
   * @return regToken
   */
  static String getRegToken() throws IOException {
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
  static NetHttpTransport getNetHttpTransport() {
    if (sNetHttpTransport == null) {
      sNetHttpTransport = new NetHttpTransport();
    }
    return sNetHttpTransport;
  }

  /**
   * Get shared AndroidJsonFactory
   * @return regToken
   */
  static AndroidJsonFactory getAndroidJsonFactory() {
    if (sAndroidJsonFactory == null) {
      sAndroidJsonFactory = new AndroidJsonFactory();
    }
    return sAndroidJsonFactory;
  }

  /**
   * Get an idToken for authorization
   * @return idToken
   */
  private static String getIdToken() {
    String idToken = "";

    // Get the IDToken that can be used securely on the backend
    // for a short time
    final GoogleSignInOptions gso =
      new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestIdToken(Msg.WEB_CLIENT_ID)
        .build();

    // Build a GoogleApiClient with access to the Google Sign-In API and the
    // options specified by gso.
    final GoogleApiClient googleApiClient =
      new GoogleApiClient.Builder(App.getContext())
        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
        .build();

    try {
      final ConnectionResult result =
        googleApiClient.blockingConnect(TIMEOUT, TimeUnit.SECONDS);
      if (result.isSuccess()) {
        final GoogleSignInResult googleSignInResult =
          Auth.GoogleSignInApi
            .silentSignIn(googleApiClient)
            .await(TIMEOUT, TimeUnit.SECONDS);
        if (googleSignInResult.isSuccess()) {
          final GoogleSignInAccount acct =
            googleSignInResult.getSignInAccount();
          if (acct != null) {
            idToken = acct.getIdToken();
          }
        }
      }
    } catch (Exception ex) {
      Log.logEx(TAG, ex.getLocalizedMessage(), ex, false);
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
  static GoogleCredential getCredential(String idToken) {

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
  static void setLocalServer(AbstractGoogleJsonClient.Builder builder) {
    if (USE_LOCAL_SERVER && BuildConfig.DEBUG) {
      // options for running against local devappserver
      // - 10.0.2.2 is localhost's IP address in Android emulator
      // - turn off compression when running against local devappserver
      builder.setRootUrl("http://10.0.2.2:8080/_ah/api/")
        .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
          @Override
          public void initialize(AbstractGoogleClientRequest<?> request) {
            request.setDisableGZipContent(true);
          }
        });
    }
  }
}
