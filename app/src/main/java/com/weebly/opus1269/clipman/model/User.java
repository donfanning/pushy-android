/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.Scopes;
import com.google.api.client.googleapis.extensions.android.gms.auth
  .GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.CoverPhoto;
import com.google.api.services.people.v1.model.Person;
import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.app.ThreadedAsyncTask;
import com.weebly.opus1269.clipman.ui.helpers.BitmapHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Singleton representing the current user */
public class User {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static User sInstance;

  public final String PREF_USER_ID = "prefUserId";
  public final String PREFS_FILENAME =
    BuildConfig.APPLICATION_ID + ".userPrefs";

  /** Global Application Context */
  private final Context mContext;

  private final String TAG = "User";

  private final String COVER_FILENAME = "cover.png";
  private final String PHOTO_FILENAME = "photo.png";

  private final String PREF_USER_NAME = "prefUserName";
  private final String PREF_USER_EMAIL = "prefUserEmail";
  private final String PREF_USER_PHOTO_URI = "prefUserPhotoUri";
  private final String PREF_USER_TYPE = "prefUserType";
  private final String PREF_USER_COVER_PHOTO_URI = "prefUserCoverPhotoUri";


  private User(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static User INST(@NonNull Context context) {
    synchronized (User.class) {
      if (sInstance == null) {
        sInstance = new User(context);
      }
      return sInstance;
    }
  }

  /** Get last signed in account */
  @Nullable
  public GoogleSignInAccount getGoogleAccount() {
    return GoogleSignIn.getLastSignedInAccount(mContext);
  }

  /**
   * Save information on current user
   * @param acct - A signedIn Google user
   */
  public void set(GoogleSignInAccount acct) {
    final Account account = acct.getAccount();

    setId(acct.getId());
    setName(acct.getDisplayName());
    setEmail(acct.getEmail());
    if (account != null) {
      setType(account.type);
    } else {
      setType("com.google");
    }
    final Uri photoUrl = acct.getPhotoUrl();
    if (photoUrl != null) {
      setPhotoUri(photoUrl.toString());
    } else {
      setPhotoUri("");
    }

    // get the avatar and cover photos from the inter-webs
    new SetPhotosAsyncTask().execute(mContext);
  }

  /** Remove information on current user */
  public void clear() {
    setId("");
    setName("");
    setEmail("");
    setPhotoUri("");
    setPhotoBitmap(null);
    setCoverPhotoUri("");
    setCoverPhotoBitmap(null);

    // clear Devices list
    Devices.INST(mContext).clear();
  }

  /**
   * Convert to own pref file.
   * Used to be stored in default shared prefs.
   */
  public void convertPrefs() {
    final String PREF_USER_PHOTO_ENCODED = "prefUserPhotoEncoded";
    final String PREF_USER_COVER_PHOTO_ENCODED = "prefUserCoverPhotoEncoded";
    final Prefs prefs = Prefs.INST(mContext);

    Log.logD(TAG, "Converting User preferences.");

    // transfer prefs
    setId(prefs.get(PREF_USER_ID, ""));
    setName(prefs.get(PREF_USER_NAME, ""));
    setEmail(prefs.get(PREF_USER_EMAIL, ""));
    setPhotoUri(prefs.get(PREF_USER_PHOTO_URI, ""));
    setCoverPhotoUri(prefs.get(PREF_USER_COVER_PHOTO_URI, ""));

    // save images
    Bitmap bitmap;
    bitmap =
      BitmapHelper.decodeBitmap(prefs.get(PREF_USER_PHOTO_ENCODED, ""));
    setPhotoBitmap(bitmap);
    bitmap = BitmapHelper.decodeBitmap(
      prefs.get(PREF_USER_COVER_PHOTO_ENCODED, ""));
    setCoverPhotoBitmap(bitmap);

    // remove old prefences
    prefs.remove(PREF_USER_ID);
    prefs.remove(PREF_USER_NAME);
    prefs.remove(PREF_USER_EMAIL);
    prefs.remove(PREF_USER_PHOTO_URI);
    prefs.remove(PREF_USER_COVER_PHOTO_URI);
    prefs.remove(PREF_USER_PHOTO_ENCODED);
    prefs.remove(PREF_USER_COVER_PHOTO_ENCODED);
  }

  private String getPref(String key, String defValue) {
    final SharedPreferences prefs =
      mContext.getSharedPreferences(PREFS_FILENAME, 0);
    return prefs.getString(key, defValue);
  }

  @SuppressLint("ApplySharedPref")
  private void setPref(String key, String value) {
    final SharedPreferences prefs =
      mContext.getSharedPreferences(PREFS_FILENAME, 0);
    prefs.edit().putString(key, value).commit();
  }

  public boolean isLoggedIn() {
    return !getId().isEmpty();
  }

  private String getId() {
    return getPref(PREF_USER_ID, "");
  }

  private void setId(String value) {
    setPref(PREF_USER_ID, value);
  }

  public String getName() {
    return getPref(PREF_USER_NAME, "");
  }

  private void setName(String value) {
    setPref(PREF_USER_NAME, value);
  }

  public String getEmail() {
    return getPref(PREF_USER_EMAIL, "");
  }

  private void setEmail(String value) {
    setPref(PREF_USER_EMAIL, value);
  }

  @SuppressWarnings("unused")
  private String getType() {
    return getPref(PREF_USER_TYPE, "com.google");
  }

  private void setType(String value) {
    setPref(PREF_USER_TYPE, value);
  }

  private String getPhotoUri() {
    return getPref(PREF_USER_PHOTO_URI, "");
  }

  private void setPhotoUri(String value) {
    setPref(PREF_USER_PHOTO_URI, value);
  }

  private Bitmap getPhotoBitmap() {
    return BitmapHelper.loadPNG(mContext, PHOTO_FILENAME);
  }

  private void setPhotoBitmap(Bitmap bitmap) {
    if (bitmap != null) {
      BitmapHelper.savePNG(mContext, PHOTO_FILENAME, bitmap);
    } else {
      BitmapHelper.deletePNG(mContext, PHOTO_FILENAME);
    }
  }

  private String getCoverPhotoUri() {
    return getPref(PREF_USER_COVER_PHOTO_URI, "");
  }

  private void setCoverPhotoUri(String value) {
    setPref(PREF_USER_COVER_PHOTO_URI, value);
  }

  private Bitmap getCoverPhotoBitmap() {
    return BitmapHelper.loadPNG(mContext, COVER_FILENAME);
  }

  private void setCoverPhotoBitmap(Bitmap bitmap) {
    if (bitmap != null) {
      BitmapHelper.savePNG(mContext, COVER_FILENAME, bitmap);
    } else {
      BitmapHelper.deletePNG(mContext, COVER_FILENAME);
    }
  }

  /**
   * Set the UI based on current user
   * @param hView - Navigation drawer Header UI component
   */
  public void setNavigationHeaderView(View hView) {
    // set users  info.
    final TextView name = hView.findViewById(R.id.personName);
    final TextView email = hView.findViewById(R.id.personEmail);

    if (isLoggedIn()) {
      // logged in. set user info.
      name.setText(getName());
      email.setText(getEmail());
    } else {
      // no login set defaults
      name.setText(R.string.default_person_name);
      email.setText("");
    }

    // set icon and background of header
    setPersonAvatar(hView);
    setCoverPhoto(hView);
  }

  private void setPersonAvatar(@NonNull View hView) {
    final ImageView personPhoto =
      hView.findViewById(R.id.personPhoto);

    if (!isLoggedIn()) {
      personPhoto.setImageResource(R.mipmap.ic_launcher);
      setPhotoBitmap(null);
      return;
    }

    final Bitmap bitmap = getPhotoBitmap();
    final boolean dark = Prefs.INST(mContext).isDarkTheme();
    if (bitmap != null) {
      // user has a photo
      final RoundedBitmapDrawable bg =
        RoundedBitmapDrawableFactory.create(
          hView.getResources(), bitmap);
      bg.setCircular(true);
      if (dark) {
        final int color = ContextCompat.getColor(
          hView.getContext(), R.color.darkener);
        bg.setColorFilter(color, PorterDuff.Mode.DARKEN);
      }
      personPhoto.setImageDrawable(bg);
    } else {
      if (dark) {
        personPhoto.setImageResource(
          R.drawable.ic_account_circle_white_24dp);
      } else {
        personPhoto.setImageResource(
          R.drawable.ic_account_circle_black_24dp);
      }
      personPhoto.setImageAlpha(200);
    }
  }

  private void setCoverPhoto(@NonNull View hView) {
    final LinearLayout coverPhoto =
      hView.findViewById(R.id.navHeader);
    final Bitmap bitmap = getCoverPhotoBitmap();
    final boolean dark = Prefs.INST(mContext).isDarkTheme();
    final Drawable drawable;

    if (isLoggedIn()) {
      if (bitmap != null) {
        // user has a cover photo
        drawable = new BitmapDrawable(hView.getResources(), bitmap);
        final int color = ContextCompat
          .getColor(hView.getContext(), R.color.darkener);
        drawable.setColorFilter(color, PorterDuff.Mode.DARKEN);
      } else {
        // no cover, use default background
        if (dark) {
          drawable = ContextCompat.getDrawable(mContext,
            R.drawable.side_nav_bar_dark);
        } else {
          drawable = ContextCompat.getDrawable(mContext,
            R.drawable.side_nav_bar);
        }
      }
    } else {
      // no cover, use default background
      if (dark) {
        drawable = ContextCompat.getDrawable(mContext,
          R.drawable.side_nav_bar_dark);
      } else {
        drawable = ContextCompat.getDrawable(mContext,
          R.drawable.side_nav_bar);
      }
      setCoverPhotoBitmap(null);
    }

    coverPhoto.setBackground(drawable);
  }

  /**
   * Inner class to handle loading of user avatar
   * and cover photo asynchronously
   */
  @SuppressLint("StaticFieldLeak")
  private class SetPhotosAsyncTask extends
    ThreadedAsyncTask<Context, Void, Void> {

    @Override
    protected Void doInBackground(Context... params) {
      Context context = params[0];
      Bitmap avatar = null;
      Bitmap cover = null;

      // Load avatar Bitmap
      if (isLoggedIn()) {
        avatar = BitmapHelper.loadBitmap(context, getPhotoUri());
      }
      setPhotoBitmap(avatar);

      // Get Cover Photo url
      String coverUrl = getCoverPhotoUrl();
      setCoverPhotoUri(coverUrl);

      // Load Cover Photo Bitmap
      if (isLoggedIn()) {
        cover = BitmapHelper.loadBitmap(context, getCoverPhotoUri());
      }
      setCoverPhotoBitmap(cover);

      return null;
    }

    private String getCoverPhotoUrl() {
      final HttpTransport httpTransport = new NetHttpTransport();
      final JacksonFactory jsonFactory = new JacksonFactory();
      Person userProfile = null;
      final Context context = mContext;
      final String email = getEmail();
      final Collection<String> scopes =
        new ArrayList<>(Collections.singletonList(Scopes.PROFILE));
      String urlName = "";

      if (TextUtils.isEmpty(email)) {
        return urlName;
      }

      GoogleAccountCredential credential =
        GoogleAccountCredential.usingOAuth2(context, scopes);
      credential.setSelectedAccount(new Account(email, "com.google"));

      PeopleService service =
        new PeopleService.Builder(httpTransport, jsonFactory, credential)
          .setApplicationName(context.getString(R.string.app_name))
          .build();

      // Get all the user details
      try {
        userProfile = service.people().get("people/me").execute();
      } catch (IOException ex) {
        Log.logEx(mContext, TAG, "", ex, false);
      }

      if (userProfile != null) {
        List<CoverPhoto> covers = userProfile.getCoverPhotos();
        if (covers != null && covers.size() > 0) {
          CoverPhoto cover = covers.get(0);
          if (cover != null) {
            urlName = cover.getUrl();
          }
        }
      }

      return urlName;
    }
  }
}
