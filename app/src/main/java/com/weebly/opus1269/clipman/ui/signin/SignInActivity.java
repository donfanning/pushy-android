/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.signin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.msg.RegistrationClient;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

/**
 * This Activity handles account selection, sign-in, registration and
 * authorization
 */
public class SignInActivity extends BaseActivity implements
  GoogleApiClient.OnConnectionFailedListener,
  OnCompleteListener<AuthResult>,
  View.OnClickListener {

  /** Result of Google signIn attempt */
  private static final int RC_SIGN_IN = 9001;

  /** Saved state: connection status */
  private static final String STATE_CONNECTION = "connection";
  /** Saved state: error message */
  private static final String STATE_ERROR = "error";

  /** Google API interface */
  private GoogleApiClient mGoogleApiClient = null;
  /** Google account */
  private GoogleSignInAccount mAccount = null;
  /** Firebase authorization */
  private FirebaseAuth mAuth = null;
  /** Receive {@link Devices} actions */
  private BroadcastReceiver mDevicesReceiver = null;

  // saved state
  /** Flag to indicate if Google API connection failed */
  private boolean mConnectionFailed = false;
  /** Error message related to SignIn or SignOut */
  private String mErrorMessage = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_sign_in;

    super.onCreate(savedInstanceState);

    // Check whether we're recreating a previously destroyed instance
    if (savedInstanceState != null) {
      // Restore value of members from saved state
      restoreInstanceState(savedInstanceState);
    }

    mDevicesReceiver = new DevicesReceiver();

    mAuth = FirebaseAuth.getInstance();

    setupButtons();

    setupGoogleSignIn();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    // Save the current state
    outState.putString(STATE_ERROR, mErrorMessage);
    outState.putBoolean(STATE_CONNECTION, mConnectionFailed);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mOptionsMenuID = R.menu.menu_signin;

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  protected void restoreInstanceState(Bundle savedInstanceState) {
    super.restoreInstanceState(savedInstanceState);

    mErrorMessage = savedInstanceState.getString(STATE_ERROR);
    mConnectionFailed = savedInstanceState.getBoolean(STATE_CONNECTION);
  }

  @Override
  protected void onStart() {
    super.onStart();

    LocalBroadcastManager.getInstance(this).registerReceiver(mDevicesReceiver,
      new IntentFilter(Intents.FILTER_DEVICES));

    updateView();

    if (User.INST(this).isLoggedIn()) {
      attemptSilentSignIn();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();

    LocalBroadcastManager.getInstance(this)
      .unregisterReceiver(mDevicesReceiver);

    dismissProgress();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    final int id = item.getItemId();
    switch (id) {
      case R.id.action_help:
        showHelpDialog();
        break;
      default:
        processed = false;
        break;
    }

    if (processed) {
      Analytics.INST(this).menuClick(TAG, item);
    }

    return processed || super.onOptionsItemSelected(item);
  }


  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    // Result returned from launching the Intent from
    // GoogleSignInApi.getSignInIntent(...);
    if (requestCode == RC_SIGN_IN) {
      final GoogleSignInResult result =
        Auth.GoogleSignInApi.getSignInResultFromIntent(data);
      handleSignInResult(result);
    } else {
      dismissProgress();
    }
  }

  @Override
  public void onComplete(@NonNull Task<AuthResult> task) {
    // Called after firebase sign in attempt
    if (!task.isSuccessful()) {
      // If sign in fails, display a message to the user.
      final Exception ex = task.getException();
      signInFailed(getString(R.string.sign_in_err_firebase), ex);
      return;
    }

    // success
    mErrorMessage = "";
    final FirebaseUser user = mAuth.getCurrentUser();
    if ((user != null) && (mAccount != null)) {
      // set User
      User.INST(this).set(mAccount);
      updateView();

      if (!Prefs.INST(this).isDeviceRegistered()) {
        // now register with server
        setProgressMessage(getString(R.string.registering));
        new RegistrationClient.RegisterAsyncTask(getApplicationContext(),
          this, mAccount.getIdToken()).executeMe();
      } else {
        dismissProgress();
      }
    } else {
      // something went wrong, shouldn't be here
      signInFailed(getString(R.string.err_unknown));
    }
  }

  @Override
  public void onClick(View v) {
    mErrorMessage = "";

    switch (v.getId()) {
      case R.id.sign_in_button:
        onSignInClicked();
        Analytics.INST(v.getContext()).buttonClick(TAG, v);
        break;
      case R.id.sign_out_button:
        onSignOutClicked();
        Analytics.INST(v.getContext()).buttonClick(TAG, v);
        break;
      default:
        break;
    }
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    // An unresolvable error has occurred and Google APIs
    // (including Sign-In) will not be available.
    connectionFailed(connectionResult);
  }

  /** SignOut of Google and Firebase */
  private void doSignOut() {
    if (mGoogleApiClient.isConnected()) {
      showProgress(getString(R.string.signing_out));
      Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
        new ResultCallback<Status>() {
          @Override
          public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
              FirebaseAuth.getInstance().signOut();
              clearUser();
              dismissProgress();
            } else {
              signOutFailed(status.getStatusMessage());
            }
          }
        });
    } else {
      signOutFailed(getString(R.string.error_connection));
    }
  }

  /** SignIn button clicked */
  private void onSignInClicked() {
    final Intent intnt = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
    startActivityForResult(intnt, RC_SIGN_IN);
  }

  /** SignOut button clicked */
  private void onSignOutClicked() {
    handleSigningOut();
  }

  /** Try to signin with cached credentials or cross-device single signin */
  private void attemptSilentSignIn() {
    final OptionalPendingResult<GoogleSignInResult> opr =
      Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

    if (opr.isDone()) {
      // If the user's cached credentials are valid,
      // the OptionalPendingResult will be "done"
      // and the GoogleSignInResult will be available instantly.
      handleSignInResult(opr.get());
    } else {
      // If the user has not previously signed in on this device
      // or the sign-in has expired,
      // this asynchronous branch will attempt to sign in the user
      // silently.  Cross-device single sign-on will occur in this branch.
      showProgress(getString(R.string.signing_in));
      opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
        @Override
        public void onResult(@NonNull GoogleSignInResult r) {
          dismissProgress();
          handleSignInResult(r);
        }
      });
    }
  }

  /**
   * All SignIn attempts will come through here
   * @param result The {@link GoogleSignInResult} of any SignIn attempt
   */
  private void handleSignInResult(GoogleSignInResult result) {
    if (!mConnectionFailed) {
      mErrorMessage = "";
      if (result.isSuccess()) {
        mAccount = result.getSignInAccount();
        if (!User.INST(this).isLoggedIn()) {
          // Authenticate with Firebase, also completes sign-in activities
          firebaseAuthWithGoogle();
        } else {
          User.INST(this).set(mAccount);
          updateView();
        }
      } else {
        // Google signIn failed
        signInFailed(result.getStatus().toString());
      }
    } else {
      signInFailed(getString(R.string.error_connection));
    }
  }

  /** Authorize with Firebase */
  private void firebaseAuthWithGoogle() {
    showProgress(getString(R.string.signing_in));
    AuthCredential credential =
      GoogleAuthProvider.getCredential(mAccount.getIdToken(), null);
    mAuth.signInWithCredential(credential)
      .addOnCompleteListener(this, this);
    // handled in onCompleteListener
  }

  /** All signout attempts come through here */
  private void handleSigningOut() {
    if (Prefs.INST(this).isDeviceRegistered()) {
      if (Prefs.INST(this).isPushClipboard()) {
        // also handles unregister and sign-out
        showProgress(getString(R.string.signing_out));
        MessagingClient.INST(this).sendDeviceRemoved();
      } else {
        // handles unregister and sign-out
        doUnregister();
      }
    } else {
      doSignOut();
    }
  }

  /** Show a dialog with rationale for signin */
  private void showHelpDialog() {
    DialogFragment dialogFragment = new HelpDialogFragment();
    dialogFragment.show(getSupportFragmentManager(), "HelpDialogFragment");
  }

  /** Initialize  Buttons */
  private void setupButtons() {
    findViewById(R.id.sign_in_button).setOnClickListener(this);
    findViewById(R.id.sign_out_button).setOnClickListener(this);

    final SignInButton signInButton = findViewById(R.id.sign_in_button);
    if (signInButton != null) {
      signInButton.setStyle(SignInButton.SIZE_WIDE, SignInButton.COLOR_AUTO);
    }
  }

  /** Initialize Google SignIn API */
  private void setupGoogleSignIn() {
    mConnectionFailed = false;
    // Configure sign-in to request the user's ID, email address, and basic
    // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
    final GoogleSignInOptions gso =
      new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestIdToken(getString(R.string.default_web_client_id))
        .build();

    // Build a GoogleApiClient with access to the Google Sign-In API and the
    // options specified by gso.
    mGoogleApiClient = new GoogleApiClient.Builder(this)
      .enableAutoManage(this, this)
      .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
      .build();
  }

  /** Unregister with App Engine. This will also perform the sign-out */
  private void doUnregister() {
    new RegistrationClient
      .UnregisterAsyncTask(getApplicationContext(), SignInActivity.this)
      .executeMe();
  }

  /** Remove all {@link User} info. */
  private void clearUser() {
    User.INST(this).clear();
    updateView();
  }

  /** Set the state of all the UI elements */
  private void updateView() {
    final View signInView = findViewById(R.id.sign_in);
    final View signOutView = findViewById(R.id.sign_out_and_disconnect);
    final TextView userNameView = findViewById(R.id.user_name);
    final TextView emailView = findViewById(R.id.email);
    final TextView errorView = findViewById(R.id.error_message);
    if (signInView == null || signOutView == null) {
      // hmm
      return;
    }

    if (User.INST(this).isLoggedIn()) {
      signInView.setVisibility(View.GONE);
      signOutView.setVisibility(View.VISIBLE);
      userNameView.setText(User.INST(this).getName());
      emailView.setVisibility(View.VISIBLE);
      emailView.setText(User.INST(this).getEmail());
    } else {
      signInView.setVisibility(View.VISIBLE);
      signOutView.setVisibility(View.GONE);
      userNameView.setText(getString(R.string.signed_out));
      emailView.setVisibility(View.GONE);
      emailView.setText("");
    }
    errorView.setText(mErrorMessage);
  }

  /**
   * Google API connection failed for some reason
   * @param result info. on failure
   */
  private void connectionFailed(@NonNull ConnectionResult result) {
    mConnectionFailed = true;
    final String title = getString(R.string.error_connection);
    mErrorMessage = title + '\n' + result.toString();
    Log.logE(this, TAG, result.toString(), title);
    clearUser();
    dismissProgress();
  }

  /**
   * SignIn failed for some reason
   * @param error info. on failure
   */
  private void signInFailed(String error) {
    final String title = getString(R.string.sign_in_err);
    mErrorMessage = title + '\n' + error;
    Log.logE(this, TAG, error, title);
    clearUser();
    dismissProgress();
  }

  /**
   * SignIn failed for some reason
   * @param ex info. on failure
   */
  private void signInFailed(String message, Exception ex) {
    final String title = getString(R.string.sign_in_err);
    mErrorMessage = title + '\n' + message;
    Log.logEx(this, TAG, message, ex, title);
    clearUser();
    dismissProgress();
  }

  /**
   * SignOut failed for some reason
   * @param error info. on failure
   */
  private void signOutFailed(String error) {
    final String title = getString(R.string.sign_out_err);
    mErrorMessage = title + '\n' + error;
    Log.logE(this, TAG, error, title);
    dismissProgress();
  }

  /**
   * Display progress view
   * @param message - message to display
   */
  private void showProgress(String message) {
    final View userView = findViewById(R.id.user);
    final View progressView = findViewById(R.id.progress);

    userView.setVisibility(View.GONE);
    progressView.setVisibility(View.VISIBLE);
    setProgressMessage(message);
  }

  /** Remove progress view */
  private void dismissProgress() {
    final View userView = findViewById(R.id.user);
    final View progressView = findViewById(R.id.progress);

    userView.setVisibility(View.VISIBLE);
    progressView.setVisibility(View.GONE);
    setProgressMessage("");
  }

  /**
   * Set progress message
   * @param message - message to display
   */
  private void setProgressMessage(String message) {
    final TextView view = findViewById(R.id.progress_message);
    view.setText(message);
  }

  /** {@link BroadcastReceiver} to handle {@link Devices} actions */
  class DevicesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      final Bundle bundle = intent.getBundleExtra(Intents.BUNDLE_DEVICES);
      if (bundle == null) {
        return;
      }
      final String action = bundle.getString(Intents.ACTION_TYPE_DEVICES);
      if (action == null) {
        return;
      }

      switch (action) {
        case Intents.TYPE_OUR_DEVICE_REMOVED:
          // device remove message sent, now unregister
          setProgressMessage(getString(R.string.unregistering));
          doUnregister();
          break;
        case Intents.TYPE_OUR_DEVICE_REGISTERED:
          // registered
          dismissProgress();
          break;
        case Intents.TYPE_OUR_DEVICE_UNREGISTERED:
          // unregistered, now signout
          doSignOut();
          break;
        case Intents.TYPE_OUR_DEVICE_REGISTER_ERROR:
          // registration error
          mErrorMessage = bundle.getString(Intents.EXTRA_TEXT);
          doSignOut();
          break;
        default:
          break;
      }
    }
  }
}



