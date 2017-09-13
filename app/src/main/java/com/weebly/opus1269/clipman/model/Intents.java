/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import com.weebly.opus1269.clipman.BuildConfig;

/**
 *
 * Static class for our intents
 */
public class Intents {
  private static final String PATH = BuildConfig.APPLICATION_ID + '.';

  /**
   * Related to {@link Devices}
   */
  public static final String FILTER_DEVICES = PATH + "filterDevices";
  public static final String BUNDLE_DEVICES = PATH + "bundleDevices";
  public static final String ACTION_TYPE_DEVICES = PATH + "actionTypeDevices";
  public static final String TYPE_UPDATE_DEVICES = PATH + "updateDevices";
  public static final String TYPE_DEVICE_REMOVED = PATH + "deviceRemoved";
  public static final String TYPE_DEVICE_REGISTERED = PATH + "deviceRegistered";
  public static final String TYPE_DEVICE_UNREGISTERED = PATH + "deviceUnregistered";
  public static final String TYPE_DEVICE_REGISTER_ERROR = PATH + "deviceRegisterError";

  /**
   * Related to {@link Notifications}
   */
  static final String ACTION_DELETE_NOTIFICATION = PATH + "deleteNotification";
  static final String ACTION_EMAIL = PATH + "email";
  static final String ACTION_SHARE = PATH + "share";
  static final String ACTION_SEARCH = PATH + "search";

  public static final String EXTRA_TEXT = PATH + "text";
  public static final String EXTRA_CLIP_COUNT = PATH + "clipCount";
  static final String EXTRA_NOTIFICATION_ID = PATH + "notificationId";
  static final String EXTRA_EMAIL_BODY = PATH + "emailBody";
  static final String EXTRA_EMAIL_SUBJECT = PATH + "emailSubject";
  public static final String EXTRA_CLIP_ITEM = PATH + "clipItem";
}
