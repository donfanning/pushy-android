/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Prefs;

/** The contract between the {@link ClipsContentProvider} and applications. */
public class ClipsContract {

  /** The authority for the clip provider */
  static final String AUTHORITY = BuildConfig.APPLICATION_ID;

  /** A content:// style uri to the authority for the clip provider */
  private static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

  private ClipsContract() {
    // To prevent someone from accidentally instantiating the contract class
  }

  /** Inner class that defines the Clip table */
  public static class Clip implements BaseColumns {
    public static final Uri CONTENT_URI = Uri.parse(AUTHORITY_URI + "/clip");
    public static final Uri CONTENT_URI_JOIN = Uri.parse(AUTHORITY_URI +
      "/clip_label_map_join");
    public static final String TABLE_NAME = "clip";
    public static final String COL_TEXT = "text";
    public static final String COL_FAV = "fav";
    public static final String COL_DATE = "date";
    public static final String COL_REMOTE = "remote";
    public static final String COL_DEVICE = "device";
    public static final String[] FULL_PROJECTION = {
      TABLE_NAME + '.' + ClipsContract.Clip._ID,
      ClipsContract.Clip.COL_TEXT,
      ClipsContract.Clip.COL_DATE,
      ClipsContract.Clip.COL_FAV,
      ClipsContract.Clip.COL_REMOTE,
      ClipsContract.Clip.COL_DEVICE
    };

    static String getDefaultSortOrder(Context context) {
      final String[] sorts =
        context.getResources().getStringArray(R.array
          .sort_type_clip_values);
      return sorts[0];
    }

    public static String getSortOrder(Context context) {
      final String[] sorts =
        context.getResources().getStringArray(R.array
          .sort_type_clip_values);
      String ret = "";
      if (Prefs.INST(context).isPinFav()) {
        ret = "fav DESC, ";
      }
      ret += sorts[Prefs.INST(context).getSortType()];
      Log.logD("ClipsContract", ret);
      return ret;
    }
  }

  /** Inner class that defines the Label table */
  public static class Label implements BaseColumns {
    public static final String TABLE_NAME = "label";
    public static final Uri CONTENT_URI = Uri.parse(AUTHORITY_URI + "/label");
    public static final String COL_NAME = "name";
    public static final String[] FULL_PROJECTION = {
      ClipsContract.Label._ID,
      ClipsContract.Label.COL_NAME,
    };

    static String getDefaultSortOrder() {
      return "LOWER(name) ASC";
    }
  }

  /**
   Inner class that defines mapping between {@link ClipItem} and {@link Label}
   */
  public static class LabelMap implements BaseColumns {
    public static final Uri CONTENT_URI =
      Uri.parse(AUTHORITY_URI + "/label_map");
    public static final String TABLE_NAME = "label_map";
    public static final String COL_CLIP_ID = "clip_id";
    public static final String COL_LABEL_NAME = "label_name";
    @SuppressWarnings("unused")
    public static final String[] FULL_PROJECTION = {
      ClipsContract.LabelMap._ID,
      ClipsContract.LabelMap.COL_CLIP_ID,
      ClipsContract.LabelMap.COL_LABEL_NAME,
    };

    static String getDefaultSortOrder() {
      return "LOWER(label_name) ASC";
    }
  }
}
