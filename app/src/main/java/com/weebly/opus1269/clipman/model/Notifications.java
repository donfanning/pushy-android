/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.annotation.SuppressLint;
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
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.msg.Msg;
import com.weebly.opus1269.clipman.services.ClipboardWatcherService;
import com.weebly.opus1269.clipman.ui.devices.DevicesActivity;
import com.weebly.opus1269.clipman.ui.errorviewer.ErrorViewerActivity;
import com.weebly.opus1269.clipman.ui.main.MainActivity;
import com.weebly.opus1269.clipman.ui.settings.SettingsActivity;

/**
 * Singleton to manage our {@link android.app.Notification} objects
 */
public class Notifications {
  // notification ids
  private static final int ID_COPY = 10;
  private static final int ID_DEVICE = 20;
  private static final int ID_CLIPBOARD_SERVICE = 30;
  private static final int ID_ERROR = 40;

  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static Notifications sInstance;

  /** Global Application Context */
  private final Context mContext;

  /** Flag for channel initialization */
  private boolean mChannelsInit = false;

  /** Count of number of clipboard messages received. */
  private int mClipItemCt;

  private Notifications(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static Notifications INST(@NonNull Context context) {
    synchronized (Notifications.class) {
      if (sInstance == null) {
        sInstance = new Notifications(context);
      }
      return sInstance;
    }
  }

  /**
   * Initialize the {@link NotificationChannel} for Android O
   * @param context A Context
   */
  @TargetApi(26)
  public void initChannels(Context context) {
    if (mChannelsInit || !AppUtils.isOreoOrLater()) {
      return;
    }

    int importance;

    importance = NotificationManager.IMPORTANCE_DEFAULT;
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

    mChannelsInit = true;
  }

  /**
   * Start and show the {@link ClipboardWatcherService}
   * @param service the foreground service to monitor the clipboard
   */
  @TargetApi(26)
  public void startAndShow(ClipboardWatcherService service) {
    final Intent intent = new Intent(mContext, SettingsActivity.class);
    final PendingIntent pIntent =
      getPIntent(mContext, SettingsActivity.class, intent);
    final String channelId = mContext.getString(R.string.channel_service);

    final Notification notification =
      new Notification.Builder(mContext, channelId)
        .setContentTitle(mContext.getString(R.string.service_title))
        .setContentText(mContext.getString(R.string.service_text))
        .setSmallIcon(R.drawable.ic_clipboard_service)
        .setLargeIcon(getLargeIcon(R.drawable.lic_local_copy))
        .setContentIntent(pIntent)
        .build();

    service.startForeground(ID_CLIPBOARD_SERVICE, notification);
  }

  /**
   * Display notification on a clipboard change
   * @param clip the {@link ClipEntity} to display notification for
   */
  public void show(ClipEntity clip) {
    final String labelFilter = Prefs.INST(mContext).getLabelFilter();
    if (ClipEntity.isWhitespace(clip) ||
      (App.isMainActivityVisible() && TextUtils.isEmpty(labelFilter)) ||
      (clip.getRemote() && !Prefs.INST(mContext).isNotifyRemote()) ||
      (!clip.getRemote() && !Prefs.INST(mContext).isNotifyLocal())) {
      return;
    }

    final String clipText = clip.getText();
    final int id = ID_COPY;

    // keep track of number of new items
    mClipItemCt++;

    final Intent intent = new Intent(mContext, MainActivity.class);
    intent.putExtra(Intents.EXTRA_CLIP, clip);
    intent.putExtra(Intents.EXTRA_CLIP_COUNT, mClipItemCt);

    PendingIntent pIntent =
      getPIntent(mContext, MainActivity.class, intent);

    final String channelId = mContext.getString(R.string.channel_message);

    // remote vs. local settings
    final int largeIcon;
    final String titleText;
    if (clip.getRemote()) {
      largeIcon = R.drawable.lic_remote_copy;
      titleText = mContext
        .getString(R.string.clip_notification_remote_fmt, clip.getDevice());
    } else {
      largeIcon = R.drawable.lic_local_copy;
      titleText = mContext.getString(R.string.clip_notification_local);
    }

    final NotificationCompat.Builder builder =
      getBuilder(pIntent, channelId, largeIcon, titleText);
    builder
      .setContentText(clipText)
      .setStyle(new NotificationCompat.BigTextStyle().bigText(clipText))
      .setWhen(clip.getDate());

    if (mClipItemCt > 1) {
      final String text =
        mContext.getString(R.string.clip_notification_count_fmt, mClipItemCt);
      builder
        .setSubText(text)
        .setNumber(mClipItemCt);
    }

    // notification deleted (cleared, swiped, etc) action
    // does not get called on tap if autocancel is true
    pIntent = NotificationReceiver
      .getPIntent(mContext, Intents.ACTION_DELETE_NOTIFICATION, id, null);
    builder.setDeleteIntent(pIntent);

    // Web Search action
    pIntent = NotificationReceiver
      .getPIntent(mContext, Intents.ACTION_SEARCH, id, clip);
    builder.addAction(R.drawable.ic_search,
      mContext.getString(R.string.action_search), pIntent);

    // Share action
    pIntent = NotificationReceiver
      .getPIntent(mContext, Intents.ACTION_SHARE, id, clip);
    builder.addAction(R.drawable.ic_share,
      mContext.getString(R.string.action_share) + " ...", pIntent);

    getManager().notify(id, builder.build());
  }

  /**
   * Display notification on remote device added or removed
   * @param action Added or removed
   * @param device remote device
   */
  public void show(String action, CharSequence device) {
    final Boolean isAdded = action.equals(Msg.ACTION_DEVICE_ADDED);
    if (TextUtils.isEmpty(action) || TextUtils.isEmpty(device) ||
      App.isDevicesActivityVisible() ||
      (isAdded && !Prefs.INST(mContext).isNotifyDeviceAdded()) ||
      (!isAdded && !Prefs.INST(mContext).isNotifyDeviceRemoved())) {
      return;
    }

    final Intent intent = new Intent(mContext, DevicesActivity.class);

    final PendingIntent pIntent =
      getPIntent(mContext, DevicesActivity.class, intent);

    // added vs. removed device settings
    final int largeIcon;
    final String channelId = mContext.getString(R.string.channel_device);
    final String titleText;
    if (isAdded) {
      largeIcon = R.drawable.lic_add_device;
      titleText = mContext.getString(R.string.device_added);
    } else {
      largeIcon = R.drawable.lic_remove_device;
      titleText = mContext.getString(R.string.device_removed);
    }

    final NotificationCompat.Builder builder =
      getBuilder(pIntent, channelId, largeIcon, titleText);
    builder
      .setContentText(device)
      .setWhen(System.currentTimeMillis());

    getManager().notify(ID_DEVICE, builder.build());
  }

  /**
   * Display notification on an app error or exception
   * @param lastError error message
   */
  public void show(LastError lastError) {
    final String message = lastError.getMessage();
    if (TextUtils.isEmpty(message) || !Prefs.INST(mContext).isNotifyError()) {
      return;
    }

    final Intent intent = new Intent(mContext, ErrorViewerActivity.class);
    intent.putExtra(Intents.EXTRA_LAST_ERROR, lastError);

    PendingIntent pIntent =
      getPIntent(mContext, ErrorViewerActivity.class, intent);

    final int id = ID_ERROR;
    final int largeIcon = R.drawable.lic_error;
    final String channelId = mContext.getString(R.string.channel_error);
    final String title = lastError.getTitle();

    final NotificationCompat.Builder builder =
      getBuilder(pIntent, channelId, largeIcon, title);
    builder
      .setContentText(message)
      .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
      .setWhen(System.currentTimeMillis());

    // change small icon
    builder.setSmallIcon(R.drawable.ic_notification_error);

    // notification deleted (cleared, swiped, etc) action
    // does not get called on tap if autocancel is true
    pIntent = NotificationReceiver.getPIntent(mContext,
      Intents.ACTION_DELETE_NOTIFICATION, id, null, null);
    builder.setDeleteIntent(pIntent);

    // Email support action
    final String emailSubject = mContext.getString(R.string.last_error);
    String emailBody = Email.INST.getBody(mContext) + lastError + " \n" +
      mContext.getString(R.string.email_error_info) + " \n \n";

    pIntent = NotificationReceiver.getPIntent(mContext,
      Intents.ACTION_EMAIL, id, emailSubject, emailBody);
    builder
      .addAction(R.drawable.ic_email, mContext.getString(R.string.action_email),
        pIntent);

    getManager().notify(id, builder.build());
  }

  /** Remove {@link ClipItem} notifications */
  public void removeClips() {
    getManager().cancel(ID_COPY);
    resetCount();
  }

  /** Remove {@link Device} notifications */
  public void removeDevices() {
    getManager().cancel(ID_DEVICE);
  }

  /** Remove {@link LastError} notifications */
  public void removeErrors() {
    getManager().cancel(ID_ERROR);
  }

  /** Remove all Notifications */
  public void removeAll() {
    removeClips();
    removeDevices();
    removeErrors();
  }

  /**
   * Display the Notification settings for Android O
   * @param context A context
   */
  @TargetApi(26)
  public void showNotificationSettings(Context context) {
    if (!AppUtils.isOreoOrLater()) {
      return;
    }

    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
    AppUtils.startNewTaskActivity(context, intent);
  }

  /** Reset message count */
  private void resetCount() {
    mClipItemCt = 0;
  }

  /**
   * Get large icon
   * @param id Resource id
   * @return new Bitmap
   */
  private Bitmap getLargeIcon(int id) {
    return BitmapFactory.decodeResource(mContext.getResources(), id);
  }

  /**
   * Get the NotificationManager
   * @return NotificationManager
   */
  private NotificationManager getManager() {
    return (NotificationManager)
      mContext.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  /**
   * Get a pending intent with a synthetic back stack
   * @param context a context
   * @param aClass  an {@link Activity} class
   * @param intent  an intent for the top of the stack
   * @return pending intent of full back stack
   */
  private PendingIntent getPIntent(Context context, Class aClass,
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
   * Get a builder with the common settings for a notification
   * @param pInt      Content intent
   * @param largeIcon display icon
   * @param titleText display title
   * @return the Builder
   */
  private NotificationCompat.Builder getBuilder(PendingIntent pInt,
                                                String channelId,
                                                int largeIcon,
                                                String titleText) {
    final NotificationCompat.Builder builder =
      new NotificationCompat.Builder(mContext, channelId);
    builder.setContentIntent(pInt)
      .setLargeIcon(getLargeIcon(largeIcon))
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(titleText)
      .setTicker(titleText)
      .setColor(ContextCompat.getColor(mContext, R.color.primary))
      .setShowWhen(true)
      .setOnlyAlertOnce(Prefs.INST(mContext).isAudibleOnce())
      .setAutoCancel(true);

    if (!AppUtils.isOreoOrLater()) {
      final Uri sound = Prefs.INST(mContext).getNotificationSound();
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
  private void createChannel(String id, int importance, String name,
                             String desc, Boolean badge) {
    if (!AppUtils.isOreoOrLater()) {
      return;
    }

    NotificationManager notificationManager = getManager();
    NotificationChannel channel = new NotificationChannel(id, name, importance);
    channel.setDescription(desc);
    channel.setShowBadge(badge);
    notificationManager.createNotificationChannel(channel);
  }

  /** {@link BroadcastReceiver} to handle notification actions */
  public static class NotificationReceiver extends BroadcastReceiver {

    /**
     * Get a pending intent for this receiver
     * @param ctxt     A Context
     * @param action   An action we know about
     * @param noteId   The id of the source notification
     * @param clip The {@link Clip}
     * @return a {@link PendingIntent}
     */
    public static PendingIntent getPIntent(Context ctxt, String action,
                                           int noteId, ClipEntity clip) {
      final Intent intent = new Intent(ctxt, NotificationReceiver.class);
      intent.setAction(action);
      intent.putExtra(Intents.EXTRA_NOTIFICATION_ID, noteId);
      intent.putExtra(Intents.EXTRA_CLIP, clip);
      return PendingIntent.getBroadcast(ctxt, 12345, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Get a pending intent for this receiver
     * @param ctxt         A Context
     * @param action       An action we know about
     * @param noteId       The id of the source notification
     * @param emailSubject subject of email
     * @param emailBody    body of email
     * @return a {@link PendingIntent}
     */
    public static PendingIntent getPIntent(Context ctxt, String action,
                                           int noteId, String emailSubject,
                                           String emailBody) {
      final Intent intent = new Intent(ctxt, NotificationReceiver.class);
      intent.setAction(action);
      intent.putExtra(Intents.EXTRA_NOTIFICATION_ID, noteId);
      intent.putExtra(Intents.EXTRA_EMAIL_SUBJECT, emailSubject);
      intent.putExtra(Intents.EXTRA_EMAIL_BODY, emailBody);
      return PendingIntent.getBroadcast(ctxt, 12345, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Remove the given notification
     * @param ctxt         A Context
     * @param notificationId notification id to remove
     */
    private void cancelNotification(Context ctxt, int notificationId) {
      if (notificationId != -1) {
        if (notificationId == ID_COPY) {
          Notifications.INST(ctxt).resetCount();
        }
        final NotificationManager notificationManager =
          Notifications.INST(ctxt).getManager();
        notificationManager.cancel(notificationId);

        // collapse notifications dialog
        ctxt.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
      }
    }

    @Override
    public void onReceive(Context ctxt, Intent intent) {
      final String action = intent.getAction();
      final ClipItem clipItem;
      final int noteId = intent.getIntExtra(Intents.EXTRA_NOTIFICATION_ID, -1);

      if (Intents.ACTION_DELETE_NOTIFICATION.equals(action)) {
        if (noteId == ID_COPY) {
          Notifications.INST(ctxt).resetCount();
        }
      } else if (Intents.ACTION_SEARCH.equals(action)) {
        clipItem = (ClipItem) intent.getSerializableExtra(
          Intents.EXTRA_CLIP_ITEM);

        // search the web for the clip text
        AppUtils.performWebSearch(ctxt, clipItem.getText());

        cancelNotification(ctxt, noteId);
      } else if (Intents.ACTION_SHARE.equals(action)) {
        clipItem = (ClipItem) intent.getSerializableExtra(
          Intents.EXTRA_CLIP_ITEM);

        // share the clip text with other apps
        clipItem.doShare(ctxt, null);

        cancelNotification(ctxt, noteId);
      } else if (Intents.ACTION_EMAIL.equals(action)) {
        final String emailSubject =
          intent.getStringExtra(Intents.EXTRA_EMAIL_SUBJECT);
        final String emailBody =
          intent.getStringExtra(Intents.EXTRA_EMAIL_BODY);

        // Send email
        Email.INST.send(ctxt, emailSubject, emailBody);

        cancelNotification(ctxt, noteId);
      }
    }
  }
}
