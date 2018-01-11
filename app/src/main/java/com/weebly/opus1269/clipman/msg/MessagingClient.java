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
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.app.ThreadedAsyncTask;
import com.weebly.opus1269.clipman.backend.messaging.Messaging;
import com.weebly.opus1269.clipman.backend.messaging.model.EndpointRet;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.devices.Device;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Prefs;

import org.json.JSONException;
import org.json.JSONObject;

/** Singleton that is the interface to our gae Messaging endpoint */
public class MessagingClient extends Endpoint {

  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static MessagingClient sInstance;

  private final String TAG = "MessagingClient";

  private final String PING;
  private final String PING_RESPONSE;
  private final String DEVICE_ADDED;
  private final String DEVICE_REMOVED;

  /** Send error message */
  private final String ERROR_SEND;

  private MessagingClient(@NonNull Context context) {
    super(context);

    PING = mContext.getString(R.string.device_ping);
    PING_RESPONSE = mContext.getString(R.string.device_ping_response);
    DEVICE_ADDED = mContext.getString(R.string.device_added);
    DEVICE_REMOVED = mContext.getString(R.string.device_removed);

    ERROR_SEND = mContext.getString(R.string.err_send);
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static MessagingClient INST(@NonNull Context context) {
    synchronized (MessagingClient.class) {
      if (sInstance == null) {
        sInstance = new MessagingClient(context);
      }
      return sInstance;
    }
  }

  /**
   * Send contents of {@link ClipItem}
   * @param clipItem - contents to send
   */
  public void send(ClipItem clipItem) {
    // Max length of fcm data message
    final int MAX_LEN = 4096;

    if (notSignedIn() || !Prefs.INST(mContext).isPushClipboard()) {
      return;
    }

    String message = clipItem.getText();
    if (message.length() > MAX_LEN) {
      // 4KB limit with FCM - server will do final limiting
      message = message.substring(0, MAX_LEN - 1);
    }
    final String favString = clipItem.isFav() ? "1" : "0";

    JSONObject data = getJSONData(Msg.ACTION_MESSAGE, message);
    try {
      if (data != null) {
        data.put(Msg.FAV, favString);
      }
    } catch (JSONException ex) {
      Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, ERROR_SEND);
      data = null;
    }

    if (data != null) {
      new MessagingAsyncTask(mContext, true).executeMe(data);
    }
  }

  /** Notify of our addition */
  void sendDeviceAdded() {
    if (notSignedIn() || !Prefs.INST(mContext).isPushClipboard()) {
      return;
    }

    JSONObject data = getJSONData(Msg.ACTION_DEVICE_ADDED, DEVICE_ADDED);
    if (data != null) {
      new MessagingAsyncTask(mContext, true).executeMe(data);
    }
  }

  /** Notify of our removal */
  public void sendDeviceRemoved() {
    if (notSignedIn() || !Prefs.INST(mContext).isPushClipboard()) {
      return;
    }

    JSONObject data = getJSONData(Msg.ACTION_DEVICE_REMOVED, DEVICE_REMOVED);
    if (data != null) {
      new MessagingAsyncTask(mContext, true).executeMe(data);
    }
  }

  /** Ping others */
  public void sendPing() {
    if (notSignedIn() || !Prefs.INST(mContext).isPushClipboard()) {
      return;
    }

    JSONObject data = getJSONData(Msg.ACTION_PING, PING);
    if (data != null) {
      new MessagingAsyncTask(mContext, true).executeMe(data);
    }
  }

  /**
   * Respond to ping
   * @param srcRegId source of ping
   */
  public void sendPingResponse(String srcRegId) {
    if (notSignedIn() || !Prefs.INST(mContext).isPushClipboard()) {
      return;
    }

    JSONObject data = getJSONData(Msg.ACTION_PING_RESPONSE, PING_RESPONSE);
    try {
      if (data != null) {
        data.put(Msg.SRC_REG_ID, srcRegId);
      }
    } catch (JSONException ex) {
      Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex,
        ERROR_SEND);
      data = null;
    }

    if (data != null) {
      new MessagingAsyncTask(mContext, true).executeMe(data);
    }
  }

  /**
   * Get an authorized connection to the MessagingEndpoint
   * @param credential - authorization for current user
   * @return Connection to MessagingEndpoint on server
   */
  private Messaging getMessagingService(GoogleCredential credential) {
    final Messaging.Builder builder =
      new Messaging.Builder(getNetHttpTransport(),
        getAndroidJsonFactory(), setHttpTimeout(credential));

    builder.setApplicationName(AppUtils.getAppName(mContext));

    // for development purposes
    setLocalServer(builder);

    return builder.build();
  }

  /**
   * Build the message data object
   * @param action  the message action
   * @param message the message text
   * @return a JSON data object
   */
  @Nullable
  private JSONObject getJSONData(String action, String message) {
    JSONObject data;
    try {
      data = new JSONObject();
      data.put(Msg.ACTION, action);
      data.put(Msg.MESSAGE, message);
      data.put(Msg.DEVICE_MODEL, Device.getMyDevice(mContext).getModel());
      data.put(Msg.DEVICE_SN, Device.getMyDevice(mContext).getSn());
      data.put(Msg.DEVICE_OS, Device.getMyDevice(mContext).getOs());
      data.put(Msg.DEVICE_NICKNAME, Device.getMyDevice(mContext).getNickname());
    } catch (JSONException ex) {
      Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex,
        ERROR_SEND);
      data = null;
    }
    return data;
  }

  /**
   * Send a message - Blocks
   * @param data Message payload
   * @return getSuccess() false on error
   */
  private EndpointRet sendMessage(JSONObject data) {
    EndpointRet ret = new EndpointRet();
    ret.setSuccess(false);
    ret.setReason(ERROR_UNKNOWN);

    try {
      final String action = data.getString(Msg.ACTION);
      final String jsonString = data.toString();

      final GoogleCredential credential = getCredential(null);
      if (credential == null) {
        ret.setReason(Log.logE(mContext, TAG, ERROR_CREDENTIAL, ERROR_SEND));
        return ret;
      }

      final Messaging msgService = getMessagingService(credential);

      // call server
      final String regToken = getRegToken();
      final Boolean highPriority =
        Prefs.INST(mContext).isHighPriority();
      ret = msgService
        .send(regToken, jsonString, highPriority).execute();
      if (ret.getSuccess()) {
        Log.logD(TAG, "Message sent to server: " + action);
        Analytics.INST(mContext).sent(action);
      } else {
        ret.setReason(
          Log.logE(mContext, TAG, ret.getReason(), ERROR_SEND));
      }
    } catch (Exception ex) {
      ret.setReason(
        Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, ERROR_SEND));
    }
    return ret;
  }

  /** AsyncTask to call GAE Messaging Endpoint */
  private static class MessagingAsyncTask extends
    ThreadedAsyncTask<JSONObject, Void, EndpointRet> {

    private final static String TAG = "MessagingAsyncTask";

    // OK, if mAppContext is the global Application context
    @SuppressLint("StaticFieldLeak")
    private final Context mAppContext;

    private final String ERROR_SEND;

    private final String ERROR_NO_SERVER_ENTRY;

    /** Max number of no devices error before disabling push */
    private final int MAX_NO_DEVICES_CT = 10;

    /** If true, retry on any error */
    private boolean mRetryOnError;

    /** Data to send */
    private JSONObject mData;

    /** Message type */
    private String mAction;

    MessagingAsyncTask(Context context, boolean retryOnError) {
      mAppContext = context;
      mRetryOnError = retryOnError;
      ERROR_SEND = context.getString(R.string.err_send);
      ERROR_NO_SERVER_ENTRY = context.getString(R.string.err_no_server_entry);

      if (mRetryOnError) {
        // skip error and exception logging on first try
        Log.disableErrorLogging();
      } else {
        Log.enableErrorLogging();
      }
    }

    @Override
    protected EndpointRet doInBackground(JSONObject... params) {
      EndpointRet ret = new EndpointRet();
      ret.setSuccess(false);
      ret.setReason(ERROR_SEND);

      try {
        mData = params[0];
        mAction = mData.getString(Msg.ACTION);

        // send message to server - blocks
        ret = MessagingClient.INST(mAppContext).sendMessage(mData);
      } catch (JSONException ex) {
        ret.setReason(Log.logEx(mAppContext, TAG, ex.getLocalizedMessage(),
          ex, ERROR_SEND));
      }
      return ret;
    }

    @Override
    protected void onPostExecute(@NonNull EndpointRet ret) {
      final String reason = ret.getReason();

      // !Important - make sure to reenable
      Log.enableErrorLogging();

      if (reason.contains(Msg.SERVER_ERR_NO_DB_ENTRY)) {
        // unrecoverable error - not in server database
        mRetryOnError = false;
        Log.logE(mAppContext, TAG, ERROR_NO_SERVER_ENTRY, ERROR_SEND);
      } else if (reason.contains(Msg.SERVER_ERR_NO_DEVICES)) {
        // unrecoverable error - no other devices registered
        mRetryOnError = false;

        // let listeners know
        Devices.INST(mAppContext).notifyNoRemoteDevicesError();

        int noDevicesCt = Prefs.INST(mAppContext).getNoDevicesCt();
        if (noDevicesCt >= MAX_NO_DEVICES_CT) {
          // disable push silently
          //Log.logE(mAppContext, TAG, "No devices", ERROR_SEND);
          if (Prefs.INST(mAppContext).isPushClipboard()) {
            Prefs.INST(mAppContext).setPushClipboard(false);
          }
        } else if (Msg.ACTION_MESSAGE.equals(mAction)) {
          // let regular messages go but increment error count
          noDevicesCt++;
          Prefs.INST(mAppContext).setNoDevicesCt(noDevicesCt);
        }
      }

      if (mRetryOnError && !ret.getSuccess()) {
        // try again on most errors
        Log.logD(TAG, "Retrying message send");
        new MessagingAsyncTask(mAppContext, false).executeMe(mData);
      } else {
        if (Msg.ACTION_DEVICE_REMOVED.equals(mAction)) {
          // remove device notification. SignInActivity will be notified that it
          // can now unregister and sign-out
          Devices.INST(mAppContext).notifyMyDeviceRemoved();
        }
      }
    }
  }
}
