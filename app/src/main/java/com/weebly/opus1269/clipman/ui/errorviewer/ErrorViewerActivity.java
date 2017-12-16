/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.errorviewer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Email;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.LastError;
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;

/** This Activity manages the {@link LastError} */
public class ErrorViewerActivity extends BaseActivity
  implements SharedPreferences.OnSharedPreferenceChangeListener {

  /** The latest error */
  private LastError mLastError;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_error_viewer;

    super.onCreate(savedInstanceState);

    // listen for preference changes
    PreferenceManager
      .getDefaultSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onResume() {
    super.onResume();

    Notifications.INST(this).removeErrors();

    // Check for intent first time
    Intent intent = getIntent();
    if (intent.hasExtra(Intents.EXTRA_LAST_ERROR)) {
      mLastError =
        (LastError) intent.getSerializableExtra(Intents.EXTRA_LAST_ERROR);
      intent.removeExtra(Intents.EXTRA_LAST_ERROR);
    } else {
      mLastError = LastError.get(this);
    }

    updateText();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mOptionsMenuID = R.menu.menu_errorviewer;

    final boolean ret = super.onCreateOptionsMenu(menu);

    updateOptionsMenu();

    return ret;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    // stop listening for preference changes
    PreferenceManager.getDefaultSharedPreferences(this)
      .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    final int id = item.getItemId();
    switch (id) {
      case R.id.action_email:
        String body = Email.INST.getBody() + mLastError + " \n" +
          getString(R.string.email_error_info) + " \n \n";
        Email.INST.send(this, getString(R.string.last_error), body);
        break;
      case R.id.action_delete:
        LastError.clear(this);
        break;
      default:
        processed = false;
        break;
    }

    if (processed) {
      Analytics.INST(this).menuClick(TAG, item);
    }

    return processed || super.onOptionsItemSelected(item);
  }

  @Override
  public void
  onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(LastError.PREF_LAST_ERROR)) {
      mLastError = LastError.get(this);
      updateText();
      updateOptionsMenu();
    }
  }

  /** Set all the {@link TextView} components */
  private void updateText() {
    TextView title = findViewById(R.id.title);
    TextView time = findViewById(R.id.time);
    TextView message = findViewById(R.id.message);
    TextView stack = findViewById(R.id.stack);

    if ((mLastError != null) && mLastError.hasMessage()) {
      title.setText(mLastError.getTitle());
      time.setText(mLastError.getRelativeTime(this));
      message.setText(mLastError.getMessage());
      stack.setText(mLastError.getStack());
    } else {
      title.setText("");
      time.setText("");
      message.setText("");
      stack.setText("");
    }
  }

  /** Set enabled state of menu items */
  private void updateOptionsMenu() {
    if (mOptionsMenu != null) {
      Boolean enabled = false;
      Integer alpha = 64;
      if ((mLastError != null) && mLastError.hasMessage()) {
        enabled = true;
        alpha = 255;
      }

      final MenuItem deleteItem = mOptionsMenu.findItem(R.id.action_delete);
      MenuTintHelper.colorMenuItem(deleteItem, null, alpha);
      deleteItem.setEnabled(enabled);

      final MenuItem emailItem = mOptionsMenu.findItem(R.id.action_email);
      MenuTintHelper.colorMenuItem(emailItem, null, alpha);
      emailItem.setEnabled(enabled);
    }
  }
}
