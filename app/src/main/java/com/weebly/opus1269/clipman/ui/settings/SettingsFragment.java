/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.takisoft.fix.support.v7.preference.EditTextPreference;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.msg.RegistrationClient;
import com.weebly.opus1269.clipman.services.ClipboardWatcherService;
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.main.MainActivity;

import java.util.HashSet;

/**
 * Fragment for app Preferences.
 * Supports Material design through {@link PreferenceFragmentCompatDividers}
 */
public class SettingsFragment extends PreferenceFragmentCompatDividers
  implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final int REQUEST_CODE_ALERT_RINGTONE = 5;

  private String mRingtoneKey;

  @Override
  public void onCreatePreferencesFix(Bundle bundle, String rootKey) {
    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preferences, rootKey);

    // listen for preference changes
    PreferenceManager
      .getDefaultSharedPreferences(getContext())
      .registerOnSharedPreferenceChangeListener(this);

    mRingtoneKey = getString(R.string.key_pref_ringtone);

    setRingtoneSummary();
    setNicknameSummary();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    try {
      return super.onCreateView(inflater, container, savedInstanceState);
    } finally {
      setDividerPreferences(
        DIVIDER_PADDING_CHILD |
          DIVIDER_CATEGORY_AFTER_LAST |
          DIVIDER_CATEGORY_BETWEEN);
    }
  }

  @Override
  public boolean onPreferenceTreeClick(Preference preference) {

    final String key = preference.getKey();

    if (mRingtoneKey.equals(key)) {
      // support library doesn't implement RingtonePreference.
      // need to do it ourselves
      // see: https://code.google.com/p/android/issues/detail?id=183255
      final Intent intent =
        new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
      intent.putExtra(
        RingtoneManager.EXTRA_RINGTONE_TYPE,
        RingtoneManager.TYPE_NOTIFICATION);
      intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
      intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
      intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
        Settings.System.DEFAULT_NOTIFICATION_URI);

      final String existingValue = Prefs.INST(getContext()).getRingtone();
      if (existingValue != null) {
        if (existingValue.isEmpty()) {
          // Select "Silent"
          intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            (Uri) null);
        } else {
          intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            Uri.parse(existingValue));
        }
      } else {
        // No ringtone has been selected, set to the default
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
          Settings.System.DEFAULT_NOTIFICATION_URI);
      }

      startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE);

      return true;
    } else if (getString(R.string.key_pref_manage_not).equals(key)) {
      // Manage notifications for Android O and later
      Notifications.showNotificationSettings(getContext());
      return true;
    }

    return super.onPreferenceTreeClick(preference);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if ((requestCode == REQUEST_CODE_ALERT_RINGTONE) && (data != null)) {
      // Save the Ringtone preference
      final Uri uri =
        data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
      String ringTone = "";

      if (uri != null) {
        ringTone = uri.toString();
      }
      Prefs.INST(getContext()).setRingtone(ringTone);
      Analytics.INST.event(((BaseActivity)getActivity()).getTAG(),
        Analytics.CAT_UI, Analytics.UI_LIST, "ringtone: " + ringTone);
      setRingtoneSummary();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    // stop listening for preference changes
    PreferenceManager.getDefaultSharedPreferences(getContext())
      .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void
  onSharedPreferenceChanged(SharedPreferences sp, String key) {
    final String keyNickname =
      getString(R.string.key_pref_nickname);
    final String keyMonitor =
      getString(R.string.key_pref_monitor_clipboard);
    final String keyTheme =
      getString(R.string.key_pref_theme);
    final String keyNotifications =
      getString(R.string.key_pref_notifications);
    final String keyReceive =
      getString(R.string.key_pref_receive_msg);
    final Activity activity = getActivity();

    // log event
    logChange(sp, key);

    // process changes
    if (key.equals(keyNickname)) {
      setNicknameSummary();
      MessagingClient.sendPing();
    } else if (key.equals(keyMonitor)) {
      // start or stop clipboard service as needed
      if (Prefs.INST(getContext()).isMonitorClipboard()) {
        ClipboardWatcherService.startService(false);
      } else {
        final Intent intent =
          new Intent(activity, ClipboardWatcherService.class);
        activity.stopService(intent);
      }
    } else if (key.equals(keyTheme)) {
      // recreate the stack so the theme is updated everywhere
      // http://stackoverflow.com/a/28799124/4468645
      TaskStackBuilder.create(activity)
        .addNextIntent(new Intent(activity, MainActivity.class))
        .addNextIntent(activity.getIntent())
        .startActivities();
    } else if (key.equals(keyNotifications)) {
      if (Prefs.INST(getContext()).notNotifications()) {
        // remove any currently displayed Notifications
        Notifications.removeAll();
      }
    } else if (key.equals(keyReceive)) {
      if (User.INST.isLoggedIn()) {
        if (Prefs.INST(getContext()).isAllowReceive()) {
          // register
          new RegistrationClient
            .RegisterAsyncTask(getActivity(), null)
            .executeMe();
        } else {
          // unregister
          new RegistrationClient
            .UnregisterAsyncTask(getActivity())
            .executeMe();
        }
      }
    }
  }

  /**
   * Log settings changes to Analytics
   * @param sp
   * @param key key of setting to log
   */
  private void logChange(SharedPreferences sp, String key) {
    final Preference preference = findPreference(key);
    final String TAG = ((BaseActivity) getActivity()).getTAG();
    final String category = Analytics.CAT_UI;
    String action = "";
    String label = "";

    // log events
    if (preference instanceof SwitchPreferenceCompat) {
      action = Analytics.UI_TOGGLE;
      label = key + ": " + sp.getBoolean(key, false);
    } else if (preference instanceof SwitchPreference) {
      action = Analytics.UI_LIST;
      label = key + ": " + sp.getBoolean(key, false);
    } else if (preference instanceof ListPreference) {
      action = Analytics.UI_LIST;
      label = key + ": " + sp.getString(key, "");
    } else if (preference instanceof MultiSelectListPreference) {
      action = Analytics.UI_MULTI_LIST;
      label = key + ": " + sp.getStringSet(key, new HashSet<String>(0));
    } else if (preference instanceof EditTextPreference) {
      action = Analytics.UI_EDIT_TEXT;
      label = key + ": " + sp.getString(key, "");
    }

    if (!TextUtils.isEmpty(action)) {
      Analytics.INST.event(TAG, category, action, label);
    }
  }

  /** Update the Ringtone summary text */
  private void setRingtoneSummary() {
    if (AppUtils.isOreoOrLater()) {
      return;
    }
    final Preference preference = findPreference(mRingtoneKey);
    final String value = Prefs.INST(getContext()).getRingtone();
    final String title;
    if (TextUtils.isEmpty(value)) {
      title = getString(R.string.key_pref_ringtone_silent);
    } else {
      final Uri uri = Uri.parse(value);
      final Ringtone ringtone =
        RingtoneManager.getRingtone(getActivity(), uri);
      title = ringtone.getTitle(getActivity());
    }
    preference.setSummary(title);
  }

  /** Update the Nickname summary text */
  private void setNicknameSummary() {
    final Preference preference =
      findPreference(getString(R.string.key_pref_nickname));
    String value = Prefs.INST(getContext()).getDeviceNickname();
    if (TextUtils.isEmpty(value)) {
      value = getResources().getString(R.string.pref_nickname_hint);
    }
    preference.setSummary(value);
  }
}
