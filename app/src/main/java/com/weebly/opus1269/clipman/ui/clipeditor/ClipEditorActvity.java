/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clipeditor;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

import org.joda.time.DateTime;

/** Activity to edit the text of a ClipItem */
public class ClipEditorActvity extends BaseActivity {

  /** Saved state for mClipItem */
  private static final String STATE_CLIP_ITEM = "clipItem";

  /** Clipitem we are editing */
  private ClipItem mClipItem;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_clip_editor;

    super.onCreate(savedInstanceState);

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setHomeAsUpIndicator(R.drawable.ic_clear);
    }

    if (savedInstanceState == null) {
      mClipItem =
        (ClipItem) getIntent().getSerializableExtra(Intents.EXTRA_CLIP_ITEM);
    } else {
      mClipItem =
        (ClipItem) savedInstanceState.getSerializable(STATE_CLIP_ITEM);
    }

    final EditText editText = findViewById(R.id.clip_text);
    if (editText != null) {
      editText.setText(mClipItem.getText());
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mOptionsMenuID = R.menu.menu_clipeditor;

    super.onCreateOptionsMenu(menu);

    return true;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putSerializable(STATE_CLIP_ITEM, mClipItem);
  }

  @Override
  public boolean onSupportNavigateUp() {
    // close this activity as opposed to navigating up
    finish();

    return false;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    final int id = item.getItemId();
    switch (id) {
      case R.id.action_discard_changes:
        finish();
        break;
      case R.id.action_save_changes:
        // TODO handle dualpane edge cases
        EditText editText = findViewById(R.id.clip_text);
        if (editText != null) {
          final CharSequence text = editText.getText();
          if (!text.toString().equals(mClipItem.getText())) {
            mClipItem.delete(this);
            if (!ClipItem.isWhitespace(mClipItem)) {
              mClipItem.setText(this, text.toString());
              mClipItem.setRemote(false);
              mClipItem.setDate(new DateTime().getMillis());
              mClipItem.save(this);
              mClipItem.send(this);
            }
          }
        }
        finish();
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
}
