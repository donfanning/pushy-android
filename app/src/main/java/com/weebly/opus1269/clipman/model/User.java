/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.accounts.Account;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.app.ThreadedAsyncTask;
import com.weebly.opus1269.clipman.ui.helpers.BitmapHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Singleton representing the current User.
 * Data backed by {@link android.support.v4.content.SharedPreferencesCompat}
 */

public enum User {
  INST;

  private static final String TAG = "User";
  public static final String PREF_USER_ID = "prefUserId";
  private static final String PREF_USER_NAME = "prefUserName";
  private static final String PREF_USER_EMAIL = "prefUserEmail";
  private static final String PREF_USER_PHOTO_URI = "prefUserPhotoUri";
  private static final String PREF_USER_PHOTO_ENCODED = "prefUserPhotoEncoded";
  private static final String PREF_USER_TYPE = "prefUserType";
  private static final String PREF_USER_COVER_PHOTO_URI =
    "prefUserCoverPhotoUri";
  private static final String PREF_USER_COVER_PHOTO_ENCODED =
    "prefUserCoverPhotoEncoded";

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
    new SetPhotosAsyncTask().execute();
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
    Devices.clear();
  }

  public boolean isLoggedIn() {
    return !getId().isEmpty();
  }

  private String getId() {
    return Prefs.get(PREF_USER_ID, "");
  }

  private void setId(String value) {
    Prefs.set(PREF_USER_ID, value);
  }

  public String getName() {
    return Prefs.get(PREF_USER_NAME, "");
  }

  private void setName(String value) {
    Prefs.set(PREF_USER_NAME, value);
  }

  public String getEmail() {
    return Prefs.get(PREF_USER_EMAIL, "");
  }

  private void setEmail(String value) {
    Prefs.set(PREF_USER_EMAIL, value);
  }

  private String getPhotoUri() {
    return Prefs.get(PREF_USER_PHOTO_URI, "");
  }

  private void setPhotoUri(String value) {
    Prefs.set(PREF_USER_PHOTO_URI, value);
  }

  private Bitmap getPhotoBitmap() {
    return BitmapHelper.decodeBitmap(
      Prefs.get(PREF_USER_PHOTO_ENCODED, ""));
  }

  private void setPhotoBitmap(Bitmap bitmap) {
    Prefs.set(PREF_USER_PHOTO_ENCODED, BitmapHelper.encodeBitmap(bitmap));
  }

  @SuppressWarnings("unused")
  public String getType() {
    return Prefs.get(PREF_USER_TYPE, "com.google");
  }

  private void setType(String value) {
    Prefs.set(PREF_USER_TYPE, value);
  }

  private String getCoverPhotoUri() {
    return Prefs.get(PREF_USER_COVER_PHOTO_URI, "");
  }

  private void setCoverPhotoUri(String value) {
    Prefs.set(PREF_USER_COVER_PHOTO_URI, value);
  }

  private Bitmap getCoverPhotoBitmap() {
    return BitmapHelper.decodeBitmap(
      Prefs.get(PREF_USER_COVER_PHOTO_ENCODED, ""));
  }

  private void setCoverPhotoBitmap(Bitmap bitmap) {
    Prefs.set(PREF_USER_COVER_PHOTO_ENCODED,
      BitmapHelper.encodeBitmap(bitmap));
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

    if (isLoggedIn()) {
      final Bitmap bitmap = getPhotoBitmap();
      final boolean dark = Prefs.isDarkTheme();
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
    } else {
      personPhoto.setImageResource(R.mipmap.ic_launcher);
      setPhotoBitmap(null);
    }
  }

  private void setCoverPhoto(@NonNull View hView) {
    final LinearLayout coverPhoto =
      hView.findViewById(R.id.navHeader);
    final Bitmap bitmap = getCoverPhotoBitmap();
    final boolean dark = Prefs.isDarkTheme();
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
          drawable = ContextCompat.getDrawable(App.getContext(),
            R.drawable.side_nav_bar_dark);
        } else {
          drawable = ContextCompat.getDrawable(App.getContext(),
            R.drawable.side_nav_bar);
        }
      }
    } else {
      // no cover, use default background
      if (dark) {
        drawable = ContextCompat.getDrawable(App.getContext(),
          R.drawable.side_nav_bar_dark);
      } else {
        drawable = ContextCompat.getDrawable(App.getContext(),
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
  private class SetPhotosAsyncTask extends ThreadedAsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... params) {
      Bitmap avatar = null;
      Bitmap cover = null;

      // Load avatar Bitmap
      if (isLoggedIn()) {
        avatar = BitmapHelper.loadBitmap(getPhotoUri());
      }
      setPhotoBitmap(avatar);

      // Get Cover Photo url
      String coverUrl = getCoverPhotoUrl();
      setCoverPhotoUri(coverUrl);

      // Load Cover Photo Bitmap
      if (isLoggedIn()) {
        cover = BitmapHelper.loadBitmap(getCoverPhotoUri());
      }
      setCoverPhotoBitmap(cover);

      return null;
    }

    private String getCoverPhotoUrl() {
      final HttpTransport httpTransport = new NetHttpTransport();
      final JacksonFactory jsonFactory = new JacksonFactory();
      Person userProfile = null;
      final Context context = App.getContext();
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
        Log.logEx(TAG, "", ex, false);
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
