/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clipviewer;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;

import java.io.Serializable;

/** This Activity manages the display of a {@link ClipItem} */
public class ClipViewerActivity extends BaseActivity implements
  ClipViewerFragment.OnClipChanged {

  /** Item from last delete operation */
  private ClipItem mUndoItem = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_clip_viewer;

    super.onCreate(savedInstanceState);

    // check if dual pane mode is active. if yes, finish this activity
    if (AppUtils.isDualPane(this)) {
      finish();
      return;
    }

    if (savedInstanceState == null) {
      // Create the viewer fragment and add it to the activity
      // using a fragment transaction.
      final Serializable clipItem =
        getIntent().getSerializableExtra(Intents.EXTRA_CLIP_ITEM);
      final String highlightText =
        getIntent().getStringExtra(Intents.EXTRA_TEXT);

      final ClipViewerFragment fragment =
        ClipViewerFragment.newInstance(clipItem, highlightText);
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
        fragment.setHighlightText(mQueryString);
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

    final int id = item.getItemId();
    switch (id) {
      case R.id.action_favorite:
        toggleFavorite();
        break;
      case R.id.action_labels:
        final Intent intent = new Intent(this, LabelsSelectActivity.class);
        intent.putExtra(Intents.EXTRA_CLIP_ITEM, this.getClipItemClone());
        AppUtils.startActivity(this, intent);
        break;
      case R.id.action_search_web:
        AppUtils.performWebSearch(this, getClipItemClone().getText());
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
  public void clipChanged(ClipItem clipItem) {
    setFabVisibility(!ClipItem.isWhitespace(clipItem));
    setTitle();
  }

  /** Set Activity title based on current {@link ClipItem} contents */
  private void setTitle() {
    final ClipItem clipItem = getClipItemClone();
    if (clipItem.isRemote()) {
      setTitle(getString(R.string.title_activity_clip_viewer_remote));
    } else {
      setTitle(getString(R.string.title_activity_clip_viewer));
    }
  }

  private ClipViewerFragment getClipViewerFragment() {
    return (ClipViewerFragment) getSupportFragmentManager()
      .findFragmentById(R.id.clip_viewer_container);
  }

  private ClipItem getClipItemClone() {
    return getClipViewerFragment().getClipItemClone();
  }

  /** Copy the {@link ClipItem} to the clipboard */
  private void copyClipItem() {
    ClipViewerFragment clipViewerFragment = getClipViewerFragment();
    if (clipViewerFragment != null) {
      clipViewerFragment.copyToClipboard();
    }
  }

  /** Delete the {@link ClipItem} from the db */
  private void deleteClipItem() {
    final ClipItem clipItem = getClipViewerFragment().getClipItemClone();
    if (clipItem == null) {
      return;
    }

    // delete from database
    final boolean deleted = clipItem.delete(this);

    String message = getResources().getString(R.string.clip_deleted);
    if (!deleted) {
      message = getResources().getString(R.string.item_delete_empty);
    } else {
      // save for undo
      mUndoItem = clipItem;
    }

    final Snackbar snack =
      Snackbar.make(findViewById(R.id.fab), message, Snackbar.LENGTH_LONG);

    if (deleted) {
      snack.setAction(R.string.button_undo, new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mUndoItem.save(v.getContext());
          Analytics.INST(v.getContext()).imageClick(TAG, getString(R.string.button_undo));
        }
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
    }

    snack.show();
  }

  /** Toggle the favortie state of the {@link ClipItem} */
  private void toggleFavorite() {
    ClipViewerFragment clipViewerFragment = getClipViewerFragment();
    ClipItem clipItem = clipViewerFragment.getClipItemClone();
    if (clipItem == null) {
      return;
    }

    clipItem.setFav(!clipItem.isFav());

    // let fragment know
    clipViewerFragment.setClipItem(clipItem);

    // update MenuItem
    setFavoriteMenuItem();

    // update database
    clipItem.save(this);
  }

  /** Set the favorite {@link MenuItem} appearence */
  private void setFavoriteMenuItem() {
    final MenuItem menuItem = mOptionsMenu.findItem(R.id.action_favorite);

    if (menuItem != null) {
      final boolean isFav = getClipItemClone().isFav();
      final int colorID = isFav ? R.color.red_500_translucent : R.color.icons;
      final int icon = isFav ? R.drawable.ic_favorite_black_24dp :
        R.drawable.ic_favorite_border_black_24dp;
      menuItem.setIcon(icon);
      final int color = ContextCompat.getColor(this, colorID);
      MenuTintHelper.colorMenuItem(menuItem, color, 255);
    }
  }
}
