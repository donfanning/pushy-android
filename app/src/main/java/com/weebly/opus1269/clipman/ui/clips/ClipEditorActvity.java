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

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.databinding.ActivityClipEditorBinding;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ErrorMsg;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.viewmodel.ClipEditorViewModel;

/** Activity to edit the text of a {@link ClipEntity} */
public class ClipEditorActvity extends BaseActivity {
  /** Our ViewModel */
  private ClipEditorViewModel mVm;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_clip_editor;
    mIsBound = true;

    super.onCreate(savedInstanceState);

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setHomeAsUpIndicator(R.drawable.ic_clear);
    }

    final Intent intent = getIntent();

    boolean addMode = false;
    ClipEntity clip =
      (ClipEntity) intent.getSerializableExtra(Intents.EXTRA_CLIP);
    if (clip == null) {
      Log.logD(TAG, "add mode");
      addMode = true;
      clip = new ClipEntity(this);
      final String labelName = intent.getStringExtra(Intents.EXTRA_TEXT);
      if (!TextUtils.isEmpty(labelName)) {
        // added from a list filtered by labelName
        // TODO
        //mClip.addLabel(this, new Label(labelName));
      }
    }

    setTitle(addMode);

    // setup ViewModel and data binding
    mVm = new ClipEditorViewModel(getApplication(), clip, addMode);
    final ActivityClipEditorBinding binding =
      (ActivityClipEditorBinding) mBinding;
    binding.setLifecycleOwner(this);
    binding.setText(mVm.getText());
    binding.setAddMode(mVm.addMode);
    binding.setVm(mVm);
    binding.executePendingBindings();

    // observe working
    mVm.getIsWorking().observe(this, isWorking -> {
      if ((isWorking != null) && !isWorking) {
        final ErrorMsg errorMsg = mVm.getErrorMsg().getValue();
        if (errorMsg != null) {
          mVm.resetErrorMsg();
        } else {
          // done with changes
          finish();
        }
      }
    });

    // observe error
    mVm.getErrorMsg().observe(this, errorMsg -> {
      if (errorMsg != null) {
        AppUtils.showMessage(this, null, errorMsg.msg);
      }
    });
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
        mVm.saveClip();
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

  private void setTitle(boolean state) {
    if (state) {
      setTitle(R.string.title_activity_clip_editor_add_mode);
    } else {
      setTitle(R.string.title_activity_clip_editor);
    }
  }
}
