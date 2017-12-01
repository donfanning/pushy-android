/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.content.res.AppCompatResources;
import android.text.TextUtils;
import android.util.Base64;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Analytics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.CheckForNull;

/**
 * Helper class for working with Bitmaps
 */
public class BitmapHelper {
  private static final String TAG = "BitmapHelper";

  private BitmapHelper() {
  }

  /**
   * Loads a Bitmap from the inter-webs, blocks
   * @param urlName path to Bitmap
   * @return The Bitmap, null on failure
   */
  @Nullable
  public static Bitmap loadBitmap(Context ctxt, String urlName) {
    final URL url;
    Bitmap bitmap = null;

    if (!TextUtils.isEmpty(urlName)) {
      try {
        url = new URL(urlName);
      } catch (final MalformedURLException ex) {
        Log.logEx(ctxt, TAG, "Bad bitmap URL", ex, false);
        return null;
      }
      InputStream inputStream = null;
      try {
        inputStream = url.openStream();
        bitmap = BitmapFactory.decodeStream(inputStream);
      } catch (final IOException ex) {
        Log.logEx(ctxt, TAG, "Failed to get bitmap", ex, false);
      } finally {
        try {
          if (inputStream != null) {
            inputStream.close();
          }
        } catch (final IOException ignore) {
          // ignore
        }
      }
    }
    return bitmap;
  }

  /**
   * Save to internal storage as .png files
   * @param ctxt  a context
   * @param filename name of file
   * @param bitmap   Bitmap to save
   */
  public static void savePNG(Context ctxt, String filename, Bitmap bitmap) {
    FileOutputStream fileOutputStream = null;
    try {
      File file = new File(ctxt.getFilesDir(), filename);
      fileOutputStream = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
    } catch (Exception ex) {
      Analytics.INST(ctxt)
        .exception(ex, "Failed to save file: " + filename);
    } finally {
      try {
        if (fileOutputStream != null) {
          fileOutputStream.close();
        }
      } catch (IOException ex) {
        Analytics.INST(ctxt)
          .exception(ex, "Failed to save file: " + filename);
      }
    }
  }

  /**
   * Load from internal storage
   * @param ctxt  a context
   * @param filename name of file
   */
  @Nullable
  public static Bitmap loadPNG(Context ctxt, String filename) {
    Bitmap bitmap = null;
    FileInputStream inputStream = null;
    try {
      File file = new File(ctxt.getFilesDir(), filename);
      if (file.exists()) {
        inputStream = new FileInputStream(file);
        bitmap = BitmapFactory.decodeStream(inputStream);
      }
    } catch (Exception ex) {
      Analytics.INST(ctxt)
        .exception(ex, "Failed to load file: " + filename);
    } finally {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException ex) {
        Analytics.INST(ctxt)
          .exception(ex, "Failed to load file: " + filename);
      }
    }
    return bitmap;
  }

  /**
   * Delete file from internal storage
   * @param ctxt  a context
   * @param filename name of file
   */
  public static boolean deletePNG(Context ctxt, String filename) {
    boolean ret = false;
    try {
      File file = new File(ctxt.getFilesDir(), filename);
      if(file.exists()) {
        ret = file.delete();
      }
    } catch (Exception ex) {
      Analytics.INST(ctxt)
        .exception(ex, "Failed to delete file: " + filename);
      ret = false;
    }
    return ret;
  }

// --Commented out by Inspection START (12/1/2017 6:11 AM):
//  /**
//   * Do a Base64 encoding of a {@link Bitmap}
//   * @param bitmap a Bitmap
//   * @return the encoded String
//   */
//  public static String encodeBitmap(@Nullable Bitmap bitmap) {
//    String encodedBitmap = "";
//    if (bitmap != null) {
//      ByteArrayOutputStream bStream = new ByteArrayOutputStream();
//      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bStream);
//      byte[] b = bStream.toByteArray();
//      encodedBitmap = Base64.encodeToString(b, Base64.DEFAULT);
//    }
//
//    return encodedBitmap;
//  }
// --Commented out by Inspection STOP (12/1/2017 6:11 AM)

  /**
   * Decode a Base64 encoding of a {@link Bitmap}
   * @param encodedBitmap a Base64 encoding of a bitmap
   * @return the decoded Bitmap null if string is empty
   */
  public static
  @CheckForNull
  Bitmap decodeBitmap(String encodedBitmap) {
    Bitmap bitmap = null;
    if (!TextUtils.isEmpty(encodedBitmap)) {
      byte[] b = Base64.decode(encodedBitmap, Base64.DEFAULT);
      bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
    }

    return bitmap;
  }

  @SuppressWarnings("unused")
  public static Bitmap getBitmapFromVectorDrawable(Context ctxt, int
    drawableId) {
    Bitmap bitmap = null;
    Drawable drawable = AppCompatResources.getDrawable(ctxt, drawableId);
    if (drawable != null) {
      if (!AppUtils.isLollipopOrLater()) {
        drawable = (DrawableCompat.wrap(drawable)).mutate();
      }

      bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
        drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
      drawable.draw(canvas);
    }
    return bitmap;
  }
}
