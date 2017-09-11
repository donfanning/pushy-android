/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.helpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.msg.Msg;
import com.weebly.opus1269.clipman.services.ClipboardWatcherService;
import com.weebly.opus1269.clipman.ui.devices.DevicesActivity;
import com.weebly.opus1269.clipman.ui.main.MainActivity;
import com.weebly.opus1269.clipman.ui.settings.SettingsActivity;

/**
 * Static class to manage our {@link android.app.Notification} objects
 */
public class NotificationHelper {
  private static final int ID_COPY = 10;
  private static final int ID_DEVICE = 20;
  private static final int ID_CLIPBOARD_SERVICE = 30;

  // Has channel been initialized
  private static boolean sChannelsInit = false;

  // keep track of number of clipboard messages received.
  private static int sClipItemCt;

  private NotificationHelper() {
  }

  ///////////////////////////////////////////////////////////////////////////
  // Public methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Initialize the {@link NotificationChannel} for Android O
   * @param context A Context
   */
  @TargetApi(26)
  public static void initChannels(Context context) {
    if (sChannelsInit || !AppUtils.isOreoOrLater()) {
      return;
    }

    int importance = NotificationManager.IMPORTANCE_DEFAULT;
    String id = context.getString(R.string.channel_message);
    String name = context.getString(R.string.channel_message_name);
    String desc = context.getString(R.string.channel_message_desc);
    createChannel(id, importance, name, desc, true);

    importance = NotificationManager.IMPORTANCE_DEFAULT;
    id = context.getString(R.string.channel_device);
    name = context.getString(R.string.channel_device_name);
    desc = context.getString(R.string.channel_device_desc);
    createChannel(id, importance, name, desc, true);

    importance = NotificationManager.IMPORTANCE_DEFAULT;
    id = context.getString(R.string.channel_error);
    name = context.getString(R.string.channel_error_name);
    desc = context.getString(R.string.channel_error_desc);
    createChannel(id, importance, name, desc, true);

    importance = NotificationManager.IMPORTANCE_LOW;
    id = context.getString(R.string.channel_service);
    name = context.getString(R.string.channel_service_name);
    desc = context.getString(R.string.channel_service_desc);
    createChannel(id, importance, name, desc, false);

    sChannelsInit = true;
  }

  /**
   * Start and show the {@link ClipboardWatcherService}
   * @param service the foreground service to minitor the clipboard
   */
  @TargetApi(26)
  public static void startAndShow(ClipboardWatcherService service) {
    final Context context = App.getContext();
    final Intent intent = new Intent(context, SettingsActivity.class);
    final PendingIntent pendingIntent =
      getPendingIntent(context, SettingsActivity.class, intent);
    final String channelId = context.getString(R.string.channel_service);

    final Notification notification =
      new Notification.Builder(context, channelId)
        .setContentTitle(context.getString(R.string.service_title))
        .setContentText(context.getString(R.string.service_text))
        .setSmallIcon(R.drawable.ic_clipboard_service)
        .setLargeIcon(getLargeIcon(R.drawable.lic_local_copy))
        .setContentIntent(pendingIntent)
        .build();

    service.startForeground(ID_CLIPBOARD_SERVICE, notification);
  }

  /**
   * Display notification on a clipboard change
   * @param clipItem the {@link ClipItem} to display notification for
   */
  public static void show(ClipItem clipItem) {
    if ((clipItem == null) ||
      TextUtils.isEmpty(clipItem.getText()) ||
      App.isMainActivityVisible() ||
      (clipItem.isRemote() && !Prefs.isNotifyRemote()) ||
      (!clipItem.isRemote() && !Prefs.isNotifyLocal())) {
      return;
    }

    final String clipText = clipItem.getText();
    final int id = ID_COPY;
    final Context context = App.getContext();

    // keep track of number of new items
    sClipItemCt++;

    final Intent intent = new Intent(context, MainActivity.class);
    intent.putExtra(AppUtils.INTENT_EXTRA_CLIP_ITEM, clipItem);
    intent.putExtra(AppUtils.INTENT_EXTRA_CLIP_COUNT, sClipItemCt);

    PendingIntent pendingIntent =
      getPendingIntent(context, MainActivity.class, intent);

    final String channelId = context.getString(R.string.channel_message);

    // remote vs. local settings
    final int largeIcon;
    final String titleText;
    if (clipItem.isRemote()) {
      largeIcon = R.drawable.lic_remote_copy;
      titleText = context.getString(R.string.clip_notification_remote_fmt, clipItem.getDevice());
    } else {
      largeIcon = R.drawable.lic_local_copy;
      titleText = context.getString(R.string.clip_notification_local);
    }

    final NotificationCompat.Builder builder =
      getBuilder(pendingIntent, channelId, largeIcon, titleText);
    builder.setContentText(clipText)
      .setStyle(new NotificationCompat.BigTextStyle().bigText(clipText))
      .setWhen(clipItem.getTime());

    if (sClipItemCt > 1) {
      builder.setSubText(context.getString(R.string.clip_notification_count_fmt, sClipItemCt)).setNumber(sClipItemCt);
    }

    // notification deleted (cleared, swiped, etc) action
    // does not get called on tap if autocancel is true
    pendingIntent = NotificationReceiver
      .getPendingIntent(AppUtils.DELETE_NOTIFICATION_ACTION, id, null);
    builder.setDeleteIntent(pendingIntent);

    // Web Search action
    pendingIntent = NotificationReceiver
      .getPendingIntent(AppUtils.SEARCH_ACTION, id, clipItem);
    builder.addAction(R.drawable.ic_search, context.getString(R.string.action_search), pendingIntent);

    // Share action
    pendingIntent = NotificationReceiver
      .getPendingIntent(AppUtils.SHARE_ACTION, id, clipItem);
    builder.addAction(R.drawable.ic_share, context.getString(R.string.action_share) + " ...", pendingIntent);

    final NotificationManager notificationManager = getManager();
    notificationManager.notify(id, builder.build());
  }

  /**
   * Display notification on remote device added or removed
   * @param action Added or removed
   * @param device remote device
   */
  public static void show(String action, CharSequence device) {
    final Boolean isAdded = action.equals(Msg.ACTION_DEVICE_ADDED);
    if (TextUtils.isEmpty(action) || TextUtils.isEmpty(device) ||
      App.isDevicesActivityVisible() ||
      (isAdded && !Prefs.isNotifyDeviceAdded()) ||
      (!isAdded && !Prefs.isNotifyDeviceRemoved())) {
      return;
    }

    final Context context = App.getContext();
    final Intent intent = new Intent(context, DevicesActivity.class);

    final PendingIntent pendingIntent =
      getPendingIntent(context, DevicesActivity.class, intent);

    // added vs. removed device settings
    final int largeIcon;
    final String channelId = context.getString(R.string.channel_device);
    final String titleText;
    if (isAdded) {
      largeIcon = R.drawable.lic_add_device;
      titleText = context.getString(R.string.device_added);
    } else {
      largeIcon = R.drawable.lic_remove_device;
      titleText = context.getString(R.string.device_removed);
    }

    final NotificationCompat.Builder builder =
      getBuilder(pendingIntent, channelId, largeIcon, titleText);
    builder
      .setContentText(device)
      .setWhen(System.currentTimeMillis());

    final NotificationManager notificationManager = getManager();
    notificationManager.notify(ID_DEVICE, builder.build());
  }

  /**
   * Remove our {@link ClipItem} notifications
   */
  public static void removeClips() {
    final NotificationManager notificationManager = getManager();
    notificationManager.cancel(ID_COPY);
    resetCount();
  }

  /**
   * Remove our {@link Device} notifications
   */
  public static void removeDevices() {
    final NotificationManager notificationManager = getManager();
    notificationManager.cancel(ID_DEVICE);
  }

  /**
   * Remove all our Notifications
   */
  public static void removeAll() {
    removeClips();
    removeDevices();
  }

  /**
   * Display the Notification settings for Android O
   * @param context A context
   */
  public static void showNotificationSettings(Context context) {
    if (!AppUtils.isOreoOrLater()) {
      return;
    }

    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
    context.startActivity(intent);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Private methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Reset count
   */
  private static void resetCount() {
    sClipItemCt = 0;
  }

  /**
   * Get large icon
   * @param id Resource id
   * @return new Bitmap
   */
  private static Bitmap getLargeIcon(int id) {
    return BitmapFactory.decodeResource(App.getContext().getResources(), id);
  }

  /**
   * Get the NotificationManager
   * @return NotificationManager
   */
  private static NotificationManager getManager() {
    final Context context = App.getContext();
    return (NotificationManager)
      context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  /**
   * Get a pending intent with a synthetic back stack
   * @param context a context
   * @param aClass  an {@link Activity} class
   * @param intent  an intent for the top of the stack
   * @return pending intent of full back stack
   */
  private static PendingIntent getPendingIntent(Context context, Class aClass,
                                                Intent intent) {
    // stupid android: http://stackoverflow.com/a/36110709/4468645
    final TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
    // Adds the back stack i.e. MainActivity.class
    stackBuilder.addParentStack(aClass);
    // Adds the Intent to the top of the stack
    stackBuilder.addNextIntent(intent);
    // Gets a PendingIntent containing the entire back stack
    return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  /**
   * Get a {@link NotificationCompat.Builder} with the shared settings
   * @param pInt      Content intent
   * @param largeIcon display icon
   * @param titleText display title
   * @return the Builder
   */
  private static NotificationCompat.Builder getBuilder(PendingIntent pInt,
                                                       String channelId,
                                                       int largeIcon,
                                                       String titleText) {
    final Context context = App.getContext();
    final NotificationCompat.Builder builder =
      new NotificationCompat.Builder(context, channelId);

    builder.setContentIntent(pInt)
      .setLargeIcon(getLargeIcon(largeIcon))
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(titleText)
      .setTicker(titleText)
      .setColor(ContextCompat.getColor(context, R.color.primary))
      .setShowWhen(true)
      .setOnlyAlertOnce(Prefs.isAudibleOnce())
      .setAutoCancel(true);

    if (!AppUtils.isOreoOrLater()) {
      final Uri sound = Prefs.getNotificationSound();
      if (sound != null) {
        builder.setSound(sound);
      }
    }

    return builder;
  }

  /**
   * Create a {@link NotificationChannel}
   * @param id         unique channel ID
   * @param importance channel importance
   * @param name       channel name
   * @param desc       channel description
   * @param badge      true if a badge should be shown on app icon
   */
  @TargetApi(26)
  private static void createChannel(String id, int importance, String name,
                                    String desc, Boolean badge) {
    NotificationManager notificationManager = getManager();
    NotificationChannel channel = new NotificationChannel(id, name, importance);
    channel.setDescription(desc);
    channel.setShowBadge(badge);
    notificationManager.createNotificationChannel(channel);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Inner classes
  ///////////////////////////////////////////////////////////////////////////

  /**
   * {@link BroadcastReceiver} to handle notification actions
   */
  public static class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      final ClipItem item;
      final int noteId =
        intent.getIntExtra(AppUtils.INTENT_EXTRA_NOTIFICATION_ID, -1);

      if (AppUtils.DELETE_NOTIFICATION_ACTION.equals(action)) {
        resetCount();
      } else if (AppUtils.SEARCH_ACTION.equals(action)) {
        item = (ClipItem) intent.getSerializableExtra(
          AppUtils.INTENT_EXTRA_CLIP_ITEM);
        // search the web for the clip text
        AppUtils.performWebSearch(item.getText());

        cancelNotification(noteId);
        // collapse notifications
        context.sendBroadcast(
          new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
      } else if (AppUtils.SHARE_ACTION.equals(action)) {
        item = (ClipItem) intent.getSerializableExtra(
          AppUtils.INTENT_EXTRA_CLIP_ITEM);
        // share the clip text with other apps
        item.doShare(null);

        cancelNotification(noteId);
        // collapse notifications
        context.sendBroadcast(
          new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
      }
    }

    /**
     * Get a pending intent for this receiver
     * @param action   An action we know about
     * @param noteId   The id of the source notification
     * @param clipItem The {@link ClipItem}
     * @return a {@link PendingIntent}
     */
    public static PendingIntent getPendingIntent(String action, int noteId,
                                                 ClipItem clipItem) {
      final Context context = App.getContext();
      final Intent intent =
        new Intent(context, NotificationReceiver.class);
      intent.setAction(action);
      intent.putExtra(AppUtils.INTENT_EXTRA_NOTIFICATION_ID, noteId);
      intent.putExtra(AppUtils.INTENT_EXTRA_CLIP_ITEM, clipItem);
      return PendingIntent
        .getBroadcast(context, 12345, intent,
          PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Remove the given notification
     * @param notificationId notification id to remove
     */
    private static void cancelNotification(int notificationId) {
      if (notificationId != -1) {
        // cancel notification
        final NotificationManager notificationManager = getManager();
        notificationManager.cancel(notificationId);
        resetCount();
      }
    }
  }
}
