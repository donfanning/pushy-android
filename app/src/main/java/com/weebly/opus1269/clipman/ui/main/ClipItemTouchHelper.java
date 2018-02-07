/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.viewmodel.MainViewModel;

/** Handle swipe to dismiss on the RecyclerView */
class ClipItemTouchHelper extends ItemTouchHelper.SimpleCallback {
  /** ViewModel of our activity */
  private final MainViewModel mMainViewModel;

  ClipItemTouchHelper(MainViewModel mainViewModel) {
    super(0, ItemTouchHelper.RIGHT);

    mMainViewModel = mainViewModel;
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
    if (direction != ItemTouchHelper.RIGHT) {
      return;
    }

    // delete item
    final ClipEntity clipEntity =
      ((ClipAdapter.ClipViewHolder) viewHolder).binding.getVm().getClipSync();
    mMainViewModel.removeClip(clipEntity);
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
}
