
/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.helpers;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Prefs;

import java.util.Collections;
import java.util.List;

/**
 * {@link Drawable} helper class.
 * @author Filipe Bezerra
 * @version 18/01/2016
 * @since 18/01/2016
 */
@SuppressWarnings("unused")
public class DrawableHelper {
  @NonNull
  private final Context mContext;
  private int mColor;
  private Drawable mDrawable;
  private Drawable mWrappedDrawable;

  private DrawableHelper(@NonNull Context context) {
    mContext = context;
  }

  /**
   * Set the enabled state appearance of an ImageView
   * @param view    The view
   * @param enabled The state
   */
  public static void setImageViewEnabled(@NonNull ImageView view,
                                         boolean enabled) {
    final int alpha = enabled ? 255 : 64;
    view.setEnabled(enabled);
    view.setImageAlpha(alpha);
  }

  /**
   * Tint an ImageView with the accent color
   * @param imageView The list
   */
  public static void tintAccentColor(@NonNull ImageView imageView) {
    final int color;
    if (Prefs.INST(imageView.getContext()).isLightTheme()) {
      color = R.color.deep_teal_500;
    } else {
      color = R.color.deep_teal_200;
    }
    tintColor(color, Collections.singletonList(imageView));
  }

  /**
   * Tint an ImageView with the primary color
   * @param imageView The list
   * @param reverse If true, reverse the color
   */
  public static void tintPrimaryColor(@NonNull ImageView imageView,
                                      boolean reverse) {
    final int color;
    boolean state = Prefs.INST(imageView.getContext()).isLightTheme();
    state = (reverse != state);
    if (state) {
      color = android.R.color.primary_text_light;
    } else {
      color = android.R.color.primary_text_dark;
    }
    tintColor(color, Collections.singletonList(imageView));
  }

  /**
   * Tint a List of ImageViews with the accent color
   * @param context   A Context
   * @param imageList The list
   */
  public static void tintAccentColor(@NonNull Context context,
                                     @NonNull List<ImageView> imageList) {
    final int color;
    if (Prefs.INST(context).isLightTheme()) {
      color = R.color.deep_teal_500;
    } else {
      color = R.color.deep_teal_200;
    }
    tintColor(color, imageList);
  }

  /**
   * Tint a List of ImageViews with the primary color
   * @param context   A Context
   * @param imageList The list
   */
  public static void tintPrimaryColor(@NonNull Context context,
                                      @NonNull List<ImageView> imageList) {
    final int color;
    if (Prefs.INST(context).isLightTheme()) {
      color = android.R.color.primary_text_light;
    } else {
      color = android.R.color.primary_text_dark;
    }
    tintColor(color, imageList);
  }

  /**
   * Tint a List of ImageViews with a color
   * @param color     A color resource id
   * @param imageList The list
   */
  private static void tintColor(int color, @NonNull List<ImageView> imageList) {
    for (final ImageView image : imageList) {
      if ((image != null) && (image.getDrawable() != null))
        DrawableHelper
          .withContext(image.getContext())
          .withColor(color)
          .withDrawable(image.getDrawable())
          .tint()
          .applyTo(image);
    }
  }

  public static DrawableHelper withContext(@NonNull Context context) {
    return new DrawableHelper(context);
  }

  public DrawableHelper withDrawable(@DrawableRes int drawableRes) {
    mDrawable = ContextCompat.getDrawable(mContext, drawableRes);
    return this;
  }

  public DrawableHelper withDrawable(@NonNull Drawable drawable) {
    mDrawable = drawable;
    return this;
  }

  public DrawableHelper withColor(@ColorRes int colorRes) {
    mColor = ContextCompat.getColor(mContext, colorRes);
    return this;
  }

  public DrawableHelper tint() {
    if (mDrawable == null) {
      throw new NullPointerException("É preciso informar o recurso drawable " +
        "pelo método withDrawable()");
    }

    if (mColor == 0) {
      throw new IllegalStateException("É necessário informar a cor a ser " +
        "definida pelo método withColor()");
    }

    mWrappedDrawable = mDrawable.mutate();
    mWrappedDrawable = DrawableCompat.wrap(mWrappedDrawable);
    DrawableCompat.setTint(mWrappedDrawable, mColor);
    DrawableCompat.setTintMode(mWrappedDrawable, PorterDuff.Mode.SRC_IN);

    return this;
  }

  @SuppressWarnings("deprecation")
  public void applyToBackground(@NonNull View view) {
    if (mWrappedDrawable == null) {
      throw new NullPointerException("É preciso chamar o método tint()");
    }

    if (AppUtils.isJellyBeanOrLater()) {
      view.setBackground(mWrappedDrawable);
    } else {
      view.setBackgroundDrawable(mWrappedDrawable);
    }
  }

  public void applyTo(@NonNull ImageView imageView) {
    if (mWrappedDrawable == null) {
      throw new NullPointerException("É preciso chamar o método tint()");
    }

    imageView.setImageDrawable(mWrappedDrawable);
  }

  public void applyTo(@NonNull MenuItem menuItem) {
    if (mWrappedDrawable == null) {
      throw new NullPointerException("É preciso chamar o método tint()");
    }

    menuItem.setIcon(mWrappedDrawable);
  }

  public void applyTo(@NonNull CheckBox checkBox) {
    if (mWrappedDrawable == null) {
      throw new NullPointerException("É preciso chamar o método tint()");
    }

    checkBox.setButtonDrawable(mWrappedDrawable);
  }

  public void applyToDrawableLeft(@NonNull CheckBox checkBox) {
    if (mWrappedDrawable == null) {
      throw new NullPointerException("É preciso chamar o método tint()");
    }

    checkBox.setCompoundDrawablesWithIntrinsicBounds(mWrappedDrawable, null,
      null, null);
  }

  public Drawable get() {
    if (mWrappedDrawable == null) {
      throw new NullPointerException("É preciso chamar o método tint()");
    }

    return mWrappedDrawable;
  }
}
