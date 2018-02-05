/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.graphics.Canvas;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.repos.MainRepo;

/** Handle swipe to dismiss on the RecyclerView */
class ClipItemTouchHelper extends ItemTouchHelper.SimpleCallback {
  /** Activity we are in */
  private final MainActivity mActivity;

  /** Item to allow delete undo */
  private UndoItem mUndoItem;

  ClipItemTouchHelper(MainActivity activity) {
    super(0, ItemTouchHelper.RIGHT);

    mActivity = activity;
    mUndoItem = null;
  }

  private static void drawBackground(RecyclerView.ViewHolder viewHolder,
                                     float dX, int actionState) {
    final View backgroundView =
      ((ClipAdapter.ClipViewHolder) viewHolder).binding.clipBackground;

    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
      //noinspection NumericCastThatLosesPrecision
      backgroundView.setRight((int) Math.max(dX, 0));
    }
  }

  @Override
  public boolean onMove(RecyclerView recyclerView,
                        RecyclerView.ViewHolder viewHolder,
                        RecyclerView.ViewHolder target) {
    // This is for drag and drop. We don't support this
    return false;
  }

  @Override
  public void onSwiped(final RecyclerView.ViewHolder viewHolder,
                       int direction) {
    if (direction == ItemTouchHelper.RIGHT) {
      // delete item
      final ClipAdapter.ClipViewHolder holder =
        (ClipAdapter.ClipViewHolder) viewHolder;
      final ClipEntity clipEntity = holder.binding.getVm().getClip().getValue();
      if (clipEntity != null) {
        final int selectedPos = mActivity.getSelectedClipPos();
        mUndoItem =
          new UndoItem(clipEntity, selectedPos, holder.itemView.isSelected());
        MainRepo.INST(App.INST()).removeClip(clipEntity);
      }

      final Snackbar snack = Snackbar
        .make(mActivity.findViewById(R.id.fab), R.string.deleted_1_item,
          Snackbar.LENGTH_LONG)
        .setAction(R.string.button_undo, v -> {
          Analytics.INST(mActivity)
            .imageClick(mActivity.getTAG(), "undoDeleteClip");
          if (mUndoItem != null) {
            mUndoItem.undo();
            mUndoItem = null;
          }
        });

      snack.show();
    }
  }

  /**
   * Delegate to the  ItemTouchUiUtil class so we can separate the itemView into
   * a foreground and background view.
   * @see <a href="https://goo.gl/Xuu1qm">ItemTouchUIUtil</a>
   * @see <a href="https://stackoverflow.com/a/35667044/4468645">My answer</a>
   */

  @Override
  public void onSelectedChanged(RecyclerView.ViewHolder viewHolder,
                                int actionState) {
    if (viewHolder != null) {
      final View foregroundView =
        ((ClipAdapter.ClipViewHolder) viewHolder).binding.clipForeground;

      getDefaultUIUtil().onSelected(foregroundView);
    }
  }

  @Override
  public void clearView(RecyclerView recyclerView,
                        RecyclerView.ViewHolder viewHolder) {
    final View backgroundView =
      ((ClipAdapter.ClipViewHolder) viewHolder).binding.clipBackground;
    final View foregroundView =
      ((ClipAdapter.ClipViewHolder) viewHolder).binding.clipForeground;

    // TODO: should animate out instead. how?
    backgroundView.setRight(0);

    getDefaultUIUtil().clearView(foregroundView);
  }

  @Override
  public void onChildDraw(Canvas c, RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          float dX, float dY, int actionState,
                          boolean isCurrentlyActive) {
    final View foregroundView =
      ((ClipAdapter.ClipViewHolder) viewHolder).binding.clipForeground;

    drawBackground(viewHolder, dX, actionState);

    getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY,
      actionState, isCurrentlyActive);
  }

  @Override
  public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                              RecyclerView.ViewHolder viewHolder,
                              float dX, float dY, int actionState,
                              boolean isCurrentlyActive) {
    final View foregroundView =
      ((ClipAdapter.ClipViewHolder) viewHolder).binding.clipForeground;

    drawBackground(viewHolder, dX, actionState);

    getDefaultUIUtil().onDrawOver(c, recyclerView, foregroundView, dX, dY,
      actionState, isCurrentlyActive);
  }

  /** Represents a deleted item that can be undone */
  private class UndoItem {
    final int mPos;
    private final ClipEntity mClipEntity;
    private final boolean mIsSelected;

    private UndoItem(ClipEntity clipEntity, int pos, boolean isSelected) {
      mClipEntity = clipEntity;
      mPos = pos;
      mIsSelected = isSelected;
    }

    private void undo() {
      MainRepo.INST(App.INST()).addClipIfNew(mClipEntity, true);

      // TODO need this?
      if (mIsSelected) {
        // little hack to make sure item is selected if it was when deleted
        //mActivity.getClipLoaderManager().getAdapter().setSelectedItemID(-1L);
        mActivity.setSelectedClipPos(mPos);
      }
    }
  }
}
