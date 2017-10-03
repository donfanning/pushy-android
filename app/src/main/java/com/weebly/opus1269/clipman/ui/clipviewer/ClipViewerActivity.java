/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clipviewer;

import android.content.Intent;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.ThreadedAsyncTask;
import com.weebly.opus1269.clipman.db.ClipsContract;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;

import java.io.Serializable;

/** This Activity manages the display of a {@link ClipItem} */
public class ClipViewerActivity extends BaseActivity implements
  ClipViewerFragment.OnClipChanged {

  // item from last delete operation
  private ClipItem mUndoItem = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_clip_viewer;

    super.onCreate(savedInstanceState);

    // check if dual pane mode is active. if yes, finish this activity
    if (AppUtils.isDualPane()) {
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
        AppUtils.performWebSearch(getClipItemClone().getText());
        break;
      case R.id.action_copy:
        copyItem();
        break;
      case R.id.action_delete:
        deleteItem();
        break;
      default:
        processed = false;
        break;
    }

    return processed || super.onOptionsItemSelected(item);
  }

  @Override
  public void onClipChanged(ClipItem clipItem) {
    setFabVisibility(!TextUtils.isEmpty(clipItem.getText()));
    setTitle();
  }

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

  private void copyItem() {
    ClipViewerFragment clipViewerFragment = getClipViewerFragment();
    if (clipViewerFragment != null) {
      clipViewerFragment.copyToClipboard();
    }
  }

  private void toggleFavorite() {
    ClipViewerFragment clipViewerFragment = getClipViewerFragment();
    ClipItem clipItem = clipViewerFragment.getClipItemClone();

    // toggle
    clipItem.setFav(!clipItem.isFav());

    // let fragment know
    clipViewerFragment.setClipItem(clipItem);

    // update MenuItem
    setFavoriteMenuItem();

    // update database
    ClipTable.INST.update(clipItem);
  }

  private void deleteItem() {
    final ClipItem clipItem = getClipViewerFragment().getClipItemClone();

    final boolean deleted = ClipTable.INST.delete(clipItem);

    // save item for undo
    mUndoItem = clipItem;

    String message = getResources().getString(R.string.clip_deleted);
    if (!deleted) {
      message = getResources().getString(R.string.item_delete_empty);
    }

    final Snackbar snack =
      Snackbar.make(findViewById(R.id.fab), message, Snackbar.LENGTH_LONG);

    if (deleted) {
      snack.setAction("UNDO", new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          ClipTable.INST.insert(mUndoItem);
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

  /**
   * Set the favorite {@link MenuItem}
   * {@link android.graphics.drawable.Drawable}
   */
  private void setFavoriteMenuItem() {
    MenuItem menuItem = mOptionsMenu.findItem(R.id.action_favorite);

    if (menuItem != null) {
      int colorID;
      if (getClipViewerFragment().getClipItemClone().isFav()) {
        menuItem.setIcon(R.drawable.ic_favorite_black_24dp);
        colorID = R.color.red_500_translucent;
      } else {
        menuItem.setIcon(R.drawable.ic_favorite_border_black_24dp);
        colorID = R.color.icons;
      }
      final int color = ContextCompat.getColor(this, colorID);
      MenuTintHelper.colorMenuItem(menuItem, color, 255);
    }
  }
}
