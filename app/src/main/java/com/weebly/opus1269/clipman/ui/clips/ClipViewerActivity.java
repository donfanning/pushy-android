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
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.databinding.ActivityClipViewerBinding;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;
import com.weebly.opus1269.clipman.viewmodel.ClipViewerViewModel;

/** This Activity manages the display of a {@link ClipEntity} */
public class ClipViewerActivity extends
  BaseActivity<ActivityClipViewerBinding> implements
  ClipViewerFragment.OnClipChanged {
  /** Our ViewModel */
  private ClipViewerViewModel mVm = null;

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

    mVm = ViewModelProviders.of(this).get(ClipViewerViewModel.class);
    final ClipViewerHandlers handlers = new ClipViewerHandlers();
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(mVm);
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

    subscribeToViewModel();

    if (savedInstanceState == null) {
      // Create the viewer fragment and add it to the activity
      // using a fragment transaction.
      final Intent intent = getIntent();
      final ClipEntity clip =
        (ClipEntity) intent.getSerializableExtra(Intents.EXTRA_CLIP);

      final String highlightText = intent.getStringExtra(Intents.EXTRA_TEXT);

      final ClipViewerFragment fragment =
        ClipViewerFragment.newInstance(clip, highlightText);
      getSupportFragmentManager().beginTransaction()
        .replace(R.id.clip_viewer_container, fragment)
        .commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mOptionsMenuID = R.menu.menu_clipviewer;

    super.onCreateOptionsMenu(menu);

    setFavoriteMenuItem();

    return true;
  }

  @Override
  protected boolean setQueryString(String queryString) {
    boolean ret = false;
    if (super.setQueryString(queryString)) {
      final ClipViewerFragment fragment = getClipViewerFragment();
      if (fragment != null) {
        fragment.setHighlight(mQueryString);
        ret = true;
      }
    }
    return ret;
  }

  @Override
  protected void onPause() {
    // TODO
    mVm.setUndoClip(null);

    super.onPause();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    Intent intent;
    final int id = item.getItemId();
    switch (id) {
      case R.id.action_favorite:
        mVm.toggleFavorite();
        break;
      case R.id.action_labels:
        intent = new Intent(this, LabelsSelectActivity.class);
        intent.putExtra(Intents.EXTRA_CLIP, getClip());
        AppUtils.startActivity(this, intent);
        break;
      case R.id.action_edit_text:
        intent = new Intent(this, ClipEditorActvity.class);
        intent.putExtra(Intents.EXTRA_CLIP, getClip());
        AppUtils.startActivity(this, intent);
        break;
      case R.id.action_search_web:
        final ClipEntity clipEntity = getClip();
        if (!ClipEntity.isWhitespace(clipEntity)) {
          AppUtils.performWebSearch(this, clipEntity.getText());
        }
        break;
      case R.id.action_copy:
        mVm.copyClip();
        break;
      case R.id.action_delete:
        deleteClip();
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
  public void clipChanged(@Nullable ClipEntity clip) {
    mVm.setClip(clip);
  }

  @Nullable
  private ClipViewerFragment getClipViewerFragment() {
    return (ClipViewerFragment)
      getSupportFragmentManager().findFragmentById(R.id.clip_viewer_container);
  }

  @Nullable
  private ClipEntity getClip() {
    return mVm == null ? null : mVm.getClipSync();
  }

  private void subscribeToViewModel() {
    // observe clip
    mVm.getClip().observe(this, clip -> {
      if (clip != null) {
        setFavoriteMenuItem();
        setTitle();
      }
    });

    // observe info message
    mVm.getInfoMessage().observe(this, infoMsg -> {
      if (!TextUtils.isEmpty(infoMsg)) {
        AppUtils.showMessage(this, mBinding.getRoot(), infoMsg);
        mVm.postInfoMessage(null);
      }
    });
  }

  /** Delete the {@link ClipEntity} from the database */
  private void deleteClip() {
    // delete it
    mVm.deleteClip();

    final Snackbar snack = Snackbar.make(mBinding.fab,
      getString(R.string.clip_deleted), Snackbar.LENGTH_LONG);
    snack.setAction(R.string.button_undo, v -> {
      mVm.undoDelete();
      Analytics.INST(v.getContext()).imageClick(TAG, "undoDeleteClip");
    }).addCallback(new Snackbar.Callback() {
      @Override
      public void onShown(Snackbar snackbar) {}

      @Override
      public void onDismissed(Snackbar snackbar, int event) {
        mVm.setUndoClip(null);
        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
          finish();
        }
      }
    });
    snack.show();
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

  /** Set the favorite {@link MenuItem} appearence */
  private void setFavoriteMenuItem() {
    if (mOptionsMenu == null) {
      return;
    }
    final MenuItem menuItem = mOptionsMenu.findItem(R.id.action_favorite);
    final ClipEntity clip = getClip();

    if ((menuItem != null) && (clip != null)) {
      final boolean isFav = clip.getFav();
      final int colorID = isFav ? R.color.red_500_translucent : R.color.icons;
      final int icon = isFav ? R.drawable.ic_favorite_black_24dp :
        R.drawable.ic_favorite_border_black_24dp;
      menuItem.setIcon(icon);
      final int color = ContextCompat.getColor(this, colorID);
      MenuTintHelper.colorMenuItem(menuItem, color, 255);
    }
  }
}
