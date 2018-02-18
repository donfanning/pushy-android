/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clips;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.databinding.ClipEditorBinding;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;
import com.weebly.opus1269.clipman.viewmodel.ClipEditorViewModel;

/** Activity to edit the text of a {@link Clip} */
public class ClipEditorActvity extends BaseActivity<ClipEditorBinding> {
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

    // setup ViewModel and data binding
    mVm = ViewModelProviders.of(this).get(ClipEditorViewModel.class);
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(mVm);
    mBinding.executePendingBindings();

    if (savedInstanceState == null) {
      final Intent intent = getIntent();
      final Clip clip = (Clip) intent.getSerializableExtra(Intents.EXTRA_CLIP);
      if (clip != null) {
        mVm.setClip(clip);
        mVm.setAddMode(false);
      }
      intent.removeExtra(Intents.EXTRA_CLIP);
    }

    setTitle();

    subscribeToViewModel();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mOptionsMenuID = R.menu.menu_clipeditor;

    super.onCreateOptionsMenu(menu);

    updateOptionMenus();

    return true;
  }

  @Override
  public boolean onSupportNavigateUp() {
    // close this activity as opposed to navigating up
    finish();
    return true;
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

  /** Observe changes to ViewModel */
  private void subscribeToViewModel() {
    // observe text
    mVm.getText().observe(this, text -> updateOptionMenus());

    // observe info message
    mVm.getInfoMessage().observe(this, infoMsg -> {
      if (!TextUtils.isEmpty(infoMsg)) {
        // our save operation succeeded.
        mVm.copyToClipboard();
        mVm.setInfoMessage("");
        finish();
      }
    });

    // observe error message
    mVm.getErrorMsg().observe(this, errorMsg -> {
      if (errorMsg != null) {
        AppUtils.showMessage(this, mBinding.getRoot(), errorMsg.msg);
      }
    });
  }

  /** Set Activity title */
  private void setTitle() {
    if (mVm.getAddMode()) {
      setTitle(R.string.title_activity_clip_editor_add_mode);
    } else {
      setTitle(R.string.title_activity_clip_editor);
    }
  }

  /** Set menu states */
  private void updateOptionMenus() {
    if (mOptionsMenu == null) {
      return;
    }

    final MenuItem saveMenu = mOptionsMenu.findItem(R.id.action_save_changes);
    if (saveMenu != null) {
      final boolean enabled = saveMenu.isEnabled();
      if (mVm.cantSave()) {
        if (enabled) {
          saveMenu.setEnabled(false);
          MenuTintHelper.colorMenuItem(saveMenu, null, 64);
        }
      } else if (!enabled) {
        saveMenu.setEnabled(true);
        MenuTintHelper.colorMenuItem(saveMenu, null, 255);
      }
    }
  }
}
