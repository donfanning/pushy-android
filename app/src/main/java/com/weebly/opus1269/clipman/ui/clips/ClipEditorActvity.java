/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clips;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.repos.MainRepo;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

import org.threeten.bp.Instant;

/** Activity to edit the text of a {@link ClipEntity} */
public class ClipEditorActvity extends BaseActivity {
  /** Saved state for mClip */
  private static final String STATE_CLIP = "clip";

  /** Saved state for mIsAddMode */
  private static final String STATE_IS_ADD_MODE = "isAddMode";

  /** Clipitem we are editing */
  private ClipEntity mClip;

  /** Are we adding a new clip instead of editing existing */
  private Boolean mIsAddMode = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_clip_editor;

    super.onCreate(savedInstanceState);

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setHomeAsUpIndicator(R.drawable.ic_clear);
    }

    if (savedInstanceState == null) {
      final Intent intent = getIntent();
      mClip = (ClipEntity) intent.getSerializableExtra(Intents.EXTRA_CLIP);
      if (mClip == null) {
        // adding new, not editing existing
        setAddMode(true);
        mClip = new ClipEntity(this);
        final String labelName = intent.getStringExtra(Intents.EXTRA_TEXT);
        if (!TextUtils.isEmpty(labelName)) {
          // added from a list filtered by labelName
          // TODO
          //mClip.addLabel(this, new Label(labelName));
        }
      }
    } else {
      mClip = (ClipEntity) savedInstanceState.getSerializable(STATE_CLIP);
      setAddMode(savedInstanceState.getBoolean(STATE_IS_ADD_MODE));
    }

    final EditText editText = findViewById(R.id.clip_text);
    if (editText != null) {
      editText.setText(mClip.getText());
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putSerializable(STATE_CLIP, mClip);
    outState.putBoolean(STATE_IS_ADD_MODE, mIsAddMode);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mOptionsMenuID = R.menu.menu_clipeditor;

    super.onCreateOptionsMenu(menu);

    return true;
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
        final EditText editText = findViewById(R.id.clip_text);
        if (editText != null) {
          final String oldText = mClip.getText();
          final String newText = editText.getText().toString();
          if (AppUtils.isWhitespace(newText)) {
            AppUtils.showMessage(this, null,
              getString(R.string.err_whitespace));
          } else if (ClipTable.INST(this).exists(newText)) {
            AppUtils.showMessage(this, null,
              getString(R.string.err_clip_exits));
          } else if (mIsAddMode || !newText.equals(oldText)) {
            if (!mIsAddMode) {
              // delete old
              MainRepo.INST(App.INST()).removeClipAsync(mClip);
            }
            // save and send new or changed
            mClip.setText(this, newText);
            mClip.setRemote(false);
            mClip.setDate(Instant.now().toEpochMilli());
            MainRepo.INST(App.INST()).addClipAsync(mClip);
            mClip.copyToClipboard(this);
            finish();
          }
        }
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

  private void setAddMode(boolean state) {
    mIsAddMode = state;
    if (state) {
      setTitle(R.string.title_activity_clip_editor_add_mode);
    } else {
      setTitle(R.string.title_activity_clip_editor);
    }
  }
}
