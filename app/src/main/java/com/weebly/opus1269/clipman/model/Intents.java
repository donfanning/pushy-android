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
  private static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
  private static final String PACKAGE_PATH = PACKAGE_NAME + '.';

  public static final String ACTION_DELETE_NOTIFICATIOn =
    PACKAGE_PATH + "ACTION_DELETE_NOTIFICATIOn";
  public static final String ACTION_EMAIL = PACKAGE_PATH + "ACTION_EMAIL";
  public static final String ACTION_SHARE = PACKAGE_PATH + "ACTION_SHARE";
  public static final String ACTION_SEARCH = PACKAGE_PATH + "ACTION_SEARCH";

  public static final String EXTRA_TEXT = PACKAGE_PATH + "TEXT";
  public static final String EXTRA_CLIP_COUNT = PACKAGE_PATH + "CLIP_COUNT";
  public static final String EXTRA_NOTIFICATION_ID =
    PACKAGE_PATH + "NOTIFICATION_ID";
  public static final String EXTRA_EMAIL_BODY = PACKAGE_PATH + "EMAIL_BODY";
  public static final String EXTRA_EMAIL_SUBJECT =
    PACKAGE_PATH + "EMAIL_SUBJECT";
  public static final String EXTRA_CLIP_ITEM = PACKAGE_PATH + "CLIP_ITEM";
}
