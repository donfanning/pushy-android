/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clips;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.Menu;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.databinding.ActivityClipViewerBinding;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.main.MainHandlers;
import com.weebly.opus1269.clipman.viewmodel.MainViewModel;

/** This Activity manages the display of a {@link ClipEntity} */
public class ClipViewerActivity extends
  BaseActivity<ActivityClipViewerBinding> {
  /** Our ViewModel */
  private MainViewModel mVm = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_clip_viewer;
    mIsBound = true;

    super.onCreate(savedInstanceState);

    // check if dual pane mode is active. if yes, finish this activity
    if (AppUtils.isDualPane(this)) {
      finish();
      return;
    }

    mVm = ViewModelProviders.of(this).get(MainViewModel.class);
    final MainHandlers handlers = new MainHandlers(this);
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(mVm);
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

    subscribeToViewModel();

    if (savedInstanceState == null) {
      // Create the viewer fragment and add it to the activity
      // using a fragment transaction.
      final ClipViewerFragment fragment = new ClipViewerFragment();
      getSupportFragmentManager().beginTransaction()
        .replace(R.id.clip_viewer_container, fragment)
        .commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    final boolean ret;
    mOptionsMenuID = R.menu.menu_clipviewer;
    ret = super.onCreateOptionsMenu(menu);
    return ret;
  }

  @Override
  protected boolean setQueryString(String queryString) {
    boolean ret = false;
    if (super.setQueryString(queryString)) {
      final ClipViewerFragment fragment = getClipViewerFragment();
      if (fragment != null) {
        fragment.setHighlightText(mQueryString);
        ret = true;
      }
    }
    return ret;
  }

  @Override
  protected void onPause() {
    // TODO
    //mVm.setUndoClip(null);

    super.onPause();
  }

  @Nullable
  private ClipViewerFragment getClipViewerFragment() {
    return (ClipViewerFragment)
      getSupportFragmentManager().findFragmentById(R.id.clip_viewer_container);
  }

  @Nullable
  private ClipEntity getClip() {
    return mVm == null ? null : mVm.getSelectedClipSync();
  }

  /** Observe changes to ViewModel */
  private void subscribeToViewModel() {
    // observe clip
    mVm.getSelectedClip().observe(this, clip -> {
      if (clip != null) {
        setTitle();
      }
    });

    // observe undo clips
    mVm.getUndoClips().observe(this, clips -> {
      if (AppUtils.isEmpty(clips)) {
        return;
      }
      final int nRows = clips.size();
      String message = nRows + getString(R.string.items_deleted);
      if (nRows == 0) {
        message = getString(R.string.item_delete_empty);
      } else if (nRows == 1) {
        message = getString(R.string.item_deleted_one);
      }

      final Snackbar snack = Snackbar.make(mBinding.fab, message, 8000);
      if (nRows > 0) {
        snack.setAction(R.string.button_undo, v -> {
          final Context ctxt = v.getContext();
          Analytics.INST(ctxt).imageClick(TAG, "undoDeleteClips");
          mVm.undoDeleteAndSelect();
        }).addCallback(new Snackbar.Callback() {

          @Override
          public void onShown(Snackbar snackbar) {
          }

          @Override
          public void onDismissed(Snackbar snackbar, int event) {
            mVm.setUndoClips(null);
            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
              finish();
            }
          }
        });
      }
      snack.show();
    });

    // observe info message
    mVm.getInfoMessage().observe(this, infoMsg -> {
      if (!TextUtils.isEmpty(infoMsg)) {
        AppUtils.showMessage(this, mBinding.getRoot(), infoMsg);
        mVm.postInfoMessage(null);
      }
    });
  }

  /** Set Activity title based on current {@link ClipEntity} contents */
  private void setTitle() {
    final ClipEntity clip = getClip();
    if ((clip != null) && clip.getRemote()) {
      setTitle(getString(R.string.title_activity_clip_viewer_remote));
    } else {
      setTitle(getString(R.string.title_activity_clip_viewer));
    }
  }
}
