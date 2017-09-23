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
import android.support.v7.widget.AppCompatDrawableManager;
import android.text.TextUtils;
import android.util.Base64;

import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;

import java.io.ByteArrayOutputStream;
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
   * @return The Bitmap null on failure
   */
  public static
  @Nullable
  Bitmap loadBitmap(String urlName) {
    final URL url;
    Bitmap bitmap = null;

    if (!TextUtils.isEmpty(urlName)) {
      try {
        url = new URL(urlName);
      } catch (final MalformedURLException ex) {
        Log.logEx(TAG, "Bad bitmap URL", ex, false);
        return null;
      }
      InputStream inputStream = null;
      try {
        inputStream = url.openStream();
        bitmap = BitmapFactory.decodeStream(inputStream);
      } catch (final IOException ex) {
        Log.logEx(TAG, "Failed to get bitmap", ex, false);
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
   * Do a Base64 encoding of a {@link Bitmap}
   * @param bitmap a Bitmap
   * @return the encoded String
   */
  public static String encodeBitmap(@Nullable Bitmap bitmap) {
    String encodedBitmap = "";
    if (bitmap != null) {
      ByteArrayOutputStream bStream = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bStream);
      byte[] b = bStream.toByteArray();
      encodedBitmap = Base64.encodeToString(b, Base64.DEFAULT);
    }

    return encodedBitmap;
  }

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
  public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
    Drawable drawable = AppCompatDrawableManager.get().getDrawable(context, drawableId);
    if (!AppUtils.isLollipopOrLater()) {
      drawable = (DrawableCompat.wrap(drawable)).mutate();
    }

    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
      drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    return bitmap;
  }

}
