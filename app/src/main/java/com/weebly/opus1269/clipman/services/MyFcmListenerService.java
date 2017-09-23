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

import android.net.Uri;
import android.os.SystemClock;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
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

/**
 * A service that listens for messages from firebase
 */
public class MyFcmListenerService extends FirebaseMessagingService {
  /** {@value} */
  private static final String TAG = "MyFcmListenerService";

  /** {@value} */
  private static final String FCM_RECEIVED = "FCM message received: ";
  /** {@value} */
  private static final String FCM_SENT = "FCM message sent: ";
  /** {@value} */
  private static final String FCM_DELETED =
    "Messages from remote devices were deleted before they could be delivered";
  /** {@value} */
  private static final String FCM_SEND_ERROR = "Error sending FCM message: ";
  /** {@value} */
  private static final String FCM_MESSAGE_ERROR =
    "Unknown FCM message received: ";

  /**
   * Save {@link ClipItem} to database and copy to clipboard
   * @param data   {@link Map} of key value pairs
   * @param device Source {@link Device}
   */
  private static void saveClipItem(Map<String, String> data, Device device) {
    final String message = data.get(Msg.MESSAGE);
    final String favString = data.get(Msg.FAV);
    final Boolean fav = "1".equals(favString);
    final String name = device.getDisplayName();
    final ClipItem clipItem =
      new ClipItem(message, new DateTime(), fav, true, name);

    // save to DB
    clipItem.save();

    // add to clipboard
    clipItem.copyToClipboard();

    // display notification if requested by user
    Notifications.show(clipItem);
  }

  @Override
  public void onCreate() {
    super.onCreate();

    // start if needed
    ClipboardWatcherService.startService(false);
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

    if (device.getUniqueName().equals(Device.getMyUniqueName())) {
      // ignore our own messages
      return;
    } else if (!User.INSTANCE.isLoggedIn()) {
      // ignore if logged out
      return;
    }

    Log.logD(TAG, FCM_RECEIVED + action);
    Analytics.INSTANCE.received(action);

    switch (action) {
      case Msg.ACTION_MESSAGE:
        // normal message, save and copy to clipboard
        Devices.add(device, false);
        saveClipItem(data, device);
        break;
      case Msg.ACTION_PING:
        // We were pinged
        Devices.add(device, true);
        MessagingClient.sendPingResponse(data.get(Msg.SRC_REG_ID));
        break;
      case Msg.ACTION_PING_RESPONSE:
        // Device responded to a ping
        Devices.add(device, true);
        Log.logD(TAG, device.getDisplayName() +
          " told me he is around.");
        break;
      case Msg.ACTION_DEVICE_ADDED:
        // A new device was added
        Devices.add(device, true);
        Notifications.show(action, device.getDisplayName());
        break;
      case Msg.ACTION_DEVICE_REMOVED:
        // A device was removed
        Devices.remove(device);
        Notifications.show(action, device.getDisplayName());
        break;
      default:
        Log.logE(TAG, action, FCM_MESSAGE_ERROR, false);
        break;
    }

    // slow down the message stream
    SystemClock.sleep(250);
  }

  @Override
  public void onDeletedMessages() {
    super.onDeletedMessages();
    final String msg = App.getContext().getString(R.string.fcm_deleted_message);
    Log.logE(TAG, msg, FCM_DELETED);
  }
}
