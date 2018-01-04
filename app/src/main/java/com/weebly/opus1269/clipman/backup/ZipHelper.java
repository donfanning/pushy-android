/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.backup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.weebly.opus1269.clipman.app.Log;

import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;

/** Singleton to manage Zip Files */
public class ZipHelper {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static ZipHelper sInstance;

  /** Global Application Context */
  private final Context mContext;

  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  private ZipHelper(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static ZipHelper INST(@NonNull Context context) {
    synchronized (ZipHelper.class) {
      if (sInstance == null) {
        sInstance = new ZipHelper(context);
      }
      return sInstance;
    }
  }

  /**
   * Create a zip file with one file as a byte array
   * @param filename name of file to add
   * @param contents contents of file to add
   * @return zip file
   */
  @Nullable
  byte[] createZipFile(@NonNull final String filename,
                       @NonNull byte[] contents) {
    final ZipEntrySource[] entries = new ZipEntrySource[]{
      new ByteSource(filename, contents)
    };
    ByteArrayOutputStream data;
    BufferedOutputStream out = null;
    try {
      data = new ByteArrayOutputStream();
      out = new BufferedOutputStream(data);
      ZipUtil.pack(entries, out);
      out.flush();
    } catch (Exception ex) {
      Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, true);
      return null;
    } finally {
      IOUtils.closeQuietly(out);
    }
    return data.toByteArray();
  }

  /**
   * Extract the data for a file from a ZipFile
   * @param filename file to extract
   * @param bis      zip file
   * @return contents of file
   */
  @Nullable
  byte[] extractFromZipFile(@NonNull final String filename,
                            @NonNull BufferedInputStream bis) {
    byte[] ret;
    try {
      ret = ZipUtil.unpackEntry(bis, filename);
    } catch (Exception ex) {
      Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, true);
      return null;
    }
    return ret;
  }
}
