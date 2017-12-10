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

package com.weebly.opus1269.clipman.services;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.msg.Msg;
import com.weebly.opus1269.clipman.model.Notifications;

import org.joda.time.DateTime;

import java.util.Map;

/** A service that listens for messages from firebase */
public class MyFcmListenerService extends FirebaseMessagingService {
  private static final String TAG = "MyFcmListenerService";

  private static final String FCM_RECEIVED = "FCM message received: ";
  private static final String FCM_DELETED =
    "Messages from remote devices were deleted before they could be delivered";
  private static final String FCM_MESSAGE_ERROR =
    "Unknown FCM message received: ";

  /**
   * Save {@link ClipItem} to database and copy to clipboard
   * @param ctxt   A Context
   * @param data   {@link Map} of key value pairs
   * @param device Source {@link Device}
   */
  private static void saveClipItem(Context ctxt, Map<String, String> data,
                                   Device device) {
    final String clipTxt = data.get(Msg.MESSAGE);
    final String favString = data.get(Msg.FAV);
    Boolean fav = "1".equals(favString);
    final String deviceName = device.getDisplayName();
    final DateTime date = new DateTime();
    final ClipItem clipItem;

    if (!fav && ClipItem.hasClipWithFav(ctxt, clipTxt)) {
      // don't override fav of an existing item
      fav = true;
    }

    clipItem = new ClipItem(ctxt, clipTxt, date, fav, true, deviceName);

    // save to DB
    clipItem.save(ctxt);

    // add to clipboard
    clipItem.copyToClipboard(ctxt);

    // display notification if requested by user
    Notifications.INST(ctxt).show(clipItem);
  }

  @Override
  public void onCreate() {
    super.onCreate();

    // start if needed
    ClipboardWatcherService.startService(this, false);
  }

  /**
   * Called when message is received from one of our devices.
   * @param message message sent from fcm.
   */
  @Override
  public void onMessageReceived(RemoteMessage message) {
    // There are two types of messages data messages and notification
    // messages. Data messages are handled
    // here in onMessageReceived whether the app is in the foreground or
    // background. Data messages are the type
    // traditionally used with GCM. Notification messages are only received
    // here in onMessageReceived when the app
    // is in the foreground. When the app is in the background an
    // automatically generated notification is displayed.
    // When the user taps on the notification they are returned to the app.
    // Messages containing both notification
    // and data payloads are treated as notification messages. The Firebase
    // console always sends notification
    // messages. For more see:
    // https://firebase.google.com/docs/cloud-messaging/concept-options

    // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

    final Map<String, String> data = message.getData();
    final String action = data.get(Msg.ACTION);
    final String model = data.get(Msg.DEVICE_MODEL);
    final String SN = data.get(Msg.DEVICE_SN);
    final String OS = data.get(Msg.DEVICE_OS);
    final String nickname = data.get(Msg.DEVICE_NICKNAME);
    final Device device = new Device(model, SN, OS, nickname);

    // decode message text
    final String msg = data.get(Msg.MESSAGE);
    if (msg != null) {
      data.put(Msg.MESSAGE, Uri.decode(msg));
    }

    if (device.getUniqueName().equals(Device.getMyUniqueName(this))) {
      // ignore our own messages
      return;
    } else if (!User.INST(this).isLoggedIn()) {
      // ignore if logged out
      return;
    }

    Log.logD(TAG, FCM_RECEIVED + action);
    Analytics.INST(this).received(action);

    switch (action) {
      case Msg.ACTION_MESSAGE:
        // normal message, save and copy to clipboard
        Devices.INST(this).add(device, false);
        saveClipItem(this, data, device);
        break;
      case Msg.ACTION_PING:
        // We were pinged
        Devices.INST(this).add(device, true);
        MessagingClient.INST(this).sendPingResponse(data.get(Msg.SRC_REG_ID));
        break;
      case Msg.ACTION_PING_RESPONSE:
        // Device responded to a ping
        Devices.INST(this).add(device, true);
        Log.logD(TAG, device.getDisplayName() +
          " told me he is around.");
        break;
      case Msg.ACTION_DEVICE_ADDED:
        // A new device was added
        Devices.INST(this).add(device, true);
        Notifications.INST(this).show(action, device.getDisplayName());
        break;
      case Msg.ACTION_DEVICE_REMOVED:
        // A device was removed
        Devices.INST(this).remove(device);
        Notifications.INST(this).show(action, device.getDisplayName());
        break;
      default:
        Log.logE(this, TAG, action, FCM_MESSAGE_ERROR, false);
        break;
    }

    // slow down the message stream
    SystemClock.sleep(250);
  }

  @Override
  public void onDeletedMessages() {
    super.onDeletedMessages();
    final String msg =
      this.getApplicationContext().getString(R.string.fcm_deleted_message);
    Log.logE(this, TAG, msg, FCM_DELETED);
  }
}
