/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.net.Uri;
import android.provider.BaseColumns;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;

/**
 * The contract between the {@link ClipContentProvider} and applications.
 */
public class ClipContract {

  // The authority for the clip provider
  static final String AUTHORITY = BuildConfig.APPLICATION_ID;

  // A content:// style uri to the authority for the clip provider
  private static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

  // To prevent someone from accidentally instantiating the contract class,
  // give it an empty constructor.
  private ClipContract() {
  }

  static String getDefaultSortOrder() {
    final String[] sorts =
      App.getContext().getResources().getStringArray(R.array.sort_type_values);
    return sorts[0];
  }

  public static String getSortOrder() {
    final String[] sorts =
      App.getContext().getResources().getStringArray(R.array.sort_type_values);
    return sorts[Prefs.getSortType()];
  }

  /* Inner class that defines the Clip table */
  @SuppressWarnings({"PublicInnerClass", "StaticInheritance", "SuperClassHasFrequentlyUsedInheritors"})
  public static class Clip implements BaseColumns {
    public static final Uri CONTENT_URI = Uri.parse(AUTHORITY_URI + "/clip");
    static final String TABLE_NAME = "clip";
    public static final String COL_TEXT = "text";
    static final String COL_DATE = "date";
    public static final String COL_FAV = "fav";
    static final String COL_REMOTE = "remote";
    static final String COL_DEVICE = "device";

    public static final String[] FULL_PROJECTION = {
      ClipContract.Clip._ID,
      ClipContract.Clip.COL_TEXT,
      ClipContract.Clip.COL_DATE,
      ClipContract.Clip.COL_FAV,
      ClipContract.Clip.COL_REMOTE,
      ClipContract.Clip.COL_DEVICE
    };

  }
}
