/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clips;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.databinding.ActivityClipViewerBinding;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.repos.MainRepo;
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

  /** Item from last delete operation */
  private ClipEntity mUndoItem = null;

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

    // setup ViewModel and data binding
    mVm = new ClipViewerViewModel(getApplication());
    final ClipViewerHandlers handlers = new ClipViewerHandlers();
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(mVm);
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

    // observe clip
    mVm.getClip().observe(this, clipEntity -> {
      if (clipEntity != null) {
        setFavoriteMenuItem();
        setTitle();
      }
    });

    if (savedInstanceState == null) {
      // Create the viewer fragment and add it to the activity
      // using a fragment transaction.
      final ClipEntity clip =
        (ClipEntity) getIntent().getSerializableExtra(Intents.EXTRA_CLIP);

      final String highlightText =
        getIntent().getStringExtra(Intents.EXTRA_TEXT);

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
    mUndoItem = null;

    super.onPause();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    Intent intent;
    final int id = item.getItemId();
    switch (id) {
      case R.id.action_favorite:
        toggleFavorite();
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
        AppUtils.performWebSearch(this, getClip().getText());
        break;
      case R.id.action_copy:
        copyClipItem();
        break;
      case R.id.action_delete:
        deleteClipItem();
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
  public void clipChanged(ClipEntity clip) {
    mVm.setClip(clip);
  }

  /** Set Activity title based on current {@link ClipEntity} contents */
  private void setTitle() {
    final ClipEntity clip = getClip();
    if (clip != null && clip.getRemote()) {
      setTitle(getString(R.string.title_activity_clip_viewer_remote));
    } else {
      setTitle(getString(R.string.title_activity_clip_viewer));
    }
  }

  private ClipViewerFragment getClipViewerFragment() {
    return (ClipViewerFragment) getSupportFragmentManager()
      .findFragmentById(R.id.clip_viewer_container);
  }

  private @Nullable
  ClipEntity getClip() {
    return getClipViewerFragment().getClip();
  }

  /** Copy the {@link ClipEntity} to the clipboard */
  private void copyClipItem() {
    ClipViewerFragment clipViewerFragment = getClipViewerFragment();
    if (clipViewerFragment != null) {
      clipViewerFragment.copyToClipboard();
    }
  }

  /** Delete the {@link ClipEntity} from the db */
  private void deleteClipItem() {
    final ClipEntity clip = getClipViewerFragment().getClip();
    if (clip == null) {
      return;
    }

    // delete from database
    MainRepo.INST(App.INST()).removeClip(clip);
    // save for undo
    mUndoItem = clip;

    String message = getResources().getString(R.string.clip_deleted);

    final Snackbar snack =
      Snackbar.make(findViewById(R.id.fab), message, Snackbar.LENGTH_LONG);

    snack.setAction(R.string.button_undo, v -> {
      MainRepo.INST(App.INST()).addClip(mUndoItem);
      Analytics.INST(v.getContext())
        .imageClick(TAG, getString(R.string.button_undo));
    }).addCallback(new Snackbar.Callback() {

      @Override
      public void onShown(Snackbar snackbar) {}

      @Override
      public void onDismissed(Snackbar snackbar, int event) {
        mUndoItem = null;
        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
          finish();
        }
      }
    });

    snack.show();
  }

  /** Toggle the favortie state of the {@link ClipEntity} */
  private void toggleFavorite() {
    ClipViewerFragment clipViewerFragment = getClipViewerFragment();
    ClipEntity clip = clipViewerFragment.getClip();
    if (clip == null) {
      return;
    }

    // update database
    clip.setFav(!clip.getFav());
    MainRepo.INST(App.INST()).updateFav(clip);
  }

  /** Set the favorite {@link MenuItem} appearence */
  private void setFavoriteMenuItem() {
    if (mOptionsMenu == null) {
      return;
    }
    final MenuItem menuItem = mOptionsMenu.findItem(R.id.action_favorite);
    final ClipEntity clipEntity = getClip();

    if (menuItem != null && clipEntity != null) {
      final boolean isFav = clipEntity.getFav();
      final int colorID = isFav ? R.color.red_500_translucent : R.color.icons;
      final int icon = isFav ? R.drawable.ic_favorite_black_24dp :
        R.drawable.ic_favorite_border_black_24dp;
      menuItem.setIcon(icon);
      final int color = ContextCompat.getColor(this, colorID);
      MenuTintHelper.colorMenuItem(menuItem, color, 255);
    }
  }
}
