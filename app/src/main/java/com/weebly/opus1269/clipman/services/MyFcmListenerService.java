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
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.MyDevice;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.msg.Msg;
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.repos.DeviceRepo;
import com.weebly.opus1269.clipman.repos.MainRepo;

import org.threeten.bp.Instant;

import java.util.Map;

/** Service that listens for messages from firebase cloud messaging */
public class MyFcmListenerService extends FirebaseMessagingService {
  private static final String TAG = "MyFcmListenerService";

  private static final String RECEIVED = "FCM message received: ";
  private static final String ERR_DELETED =
    "Messages from remote devices were deleted before they could be delivered";
  private static final String ERR_UNKNOWN =
    "Unknown FCM message received: ";
  private static final String ERR_SAVE =
    "Failed to save clip from remote device";

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

    if (!User.INST(this).isLoggedIn()) {
      // ignore if logged out
      return;
    }

    final Map<String, String> data = message.getData();
    final String action = data.get(Msg.ACTION);
    final String model = data.get(Msg.DEVICE_MODEL);
    final String SN = data.get(Msg.DEVICE_SN);
    final String OS = data.get(Msg.DEVICE_OS);
    final String nickname = data.get(Msg.DEVICE_NICKNAME);
    final DeviceEntity device = new DeviceEntity(model, SN, OS, nickname);

    // decode message text
    final String msg = data.get(Msg.MESSAGE);
    if (msg != null) {
      data.put(Msg.MESSAGE, Uri.decode(msg));
    }

    if (device.getUniqueName().equals(MyDevice.INST(this).getUniqueName())) {
      // ignore our own messages
      return;
    }

    Log.logD(TAG, RECEIVED + action);
    Analytics.INST(this).received(action);

    switch (action) {
      case Msg.ACTION_MESSAGE:
        // normal message, save and copy to clipboard
        DeviceRepo.INST(getApplication()).add(device);
        saveClip(data, device);
        break;
      case Msg.ACTION_PING:
        // We were pinged
        DeviceRepo.INST(getApplication()).add(device);
        MessagingClient.INST(this).sendPingResponse(data.get(Msg.SRC_REG_ID));
        break;
      case Msg.ACTION_PING_RESPONSE:
        // Device responded to a ping
        DeviceRepo.INST(getApplication()).add(device);
        Log.logD(TAG, device.getDisplayName() +
          " told me he is around.");
        break;
      case Msg.ACTION_DEVICE_ADDED:
        // A new device was added
        DeviceRepo.INST(getApplication()).add(device);
        Notifications.INST(this).show(action, device.getDisplayName());
        break;
      case Msg.ACTION_DEVICE_REMOVED:
        // A device was removed
        DeviceRepo.INST(getApplication()).remove(device);
        Notifications.INST(this).show(action, device.getDisplayName());
        break;
      default:
        Log.logE(this, TAG, action, ERR_UNKNOWN, false);
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
    Log.logE(this, TAG, msg, ERR_DELETED);
  }


  /**
   * Save {@link ClipEntity} to the database and copy to clipboard
   * @param data   {@link Map} of key value pairs
   * @param device Source {@link Device}
   */
  private void saveClip(Map<String, String> data, DeviceEntity device) {
    final String clipTxt = data.get(Msg.MESSAGE);
    final boolean fav = "1".equals(data.get(Msg.FAV));
    final String dName = device.getDisplayName();
    final long date = Instant.now().toEpochMilli();
    final ClipEntity clip = new ClipEntity(clipTxt, date, fav, true, dName);

    long id = MainRepo.INST(App.INST()).addClipSync(clip);
    if (id != -1L) {
      clip.setId(id);
      clip.copyToClipboard(this);
      Notifications.INST(this).show(clip);
    } else {
      Log.logE(this, TAG, ERR_SAVE);
    }
  }
}
