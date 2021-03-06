/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.androidessence.recyclerviewcursoradapter
  .RecyclerViewCursorViewHolder;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.ClipsContract;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/** Bridge between the main RecyclerView and the Clips.db database */
class ClipCursorAdapter extends
  RecyclerViewCursorAdapter<ClipCursorAdapter.ClipViewHolder> {

  private final MainActivity mActivity;
  // The currently selected position in the list
  private int mSelectedPos = 0;
  // The database _ID of the selection list item
  private long mSelectedItemID = -1L;

  ClipCursorAdapter(MainActivity activity) {
    super(activity);

    mActivity = activity;

    // needed to allow animations to run
    setHasStableIds(true);

    setupCursorAdapter(null, 0, R.layout.clip_row, false);
  }

  /** Returns the ViewHolder to use for this adapter. */
  @Override
  public ClipViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view =
      mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent);
    final ClipViewHolder holder = new ClipViewHolder(view);

    holder.itemView.setOnClickListener(mActivity.getClipLoaderManager());
    holder.favCheckBox.setOnClickListener(mActivity.getClipLoaderManager());
    holder.copyButton.setOnClickListener(mActivity.getClipLoaderManager());
    holder.labelButton.setOnClickListener(mActivity.getClipLoaderManager());

    return holder;
  }

  /**
   * Moves the Cursor of the CursorAdapter to the appropriate position and binds
   * the view for that item.
   */
  @Override
  public void onBindViewHolder(ClipViewHolder holder, int position) {
    // Move cursor to this position
    mCursorAdapter.getCursor().moveToPosition(position);

    // Set the ViewHolder
    setViewHolder(holder);

    // Bind this view
    mCursorAdapter.bindView(null, mContext, mCursorAdapter.getCursor());

    // color the icons
    tintIcons(holder);

    if (AppUtils.isDualPane(mContext)) {
      // set selected state of the view
      if (getSelectedPos() == position) {
        if (!holder.itemView.isSelected()) {
          holder.itemView.setSelected(true);
        }
      } else {
        holder.itemView.setSelected(false);
      }
    }
  }

  @Override
  // needed to allow animations to run
  public long getItemId(int position) {
    return mCursorAdapter.getItemId(position);
  }

  /**
   * We are a delegate for the savedInstanceState of the {@link MainActivity}
   * Here we are restoring the selected state
   * @param pos selected position in list
   * @param id  DB id of selected row
   */
  void restoreSelection(int pos, long id) {
    mSelectedPos = pos;
    mSelectedItemID = id;
  }

  /** Get position in view from db PK */
  int getPosFromItemID(long itemID) {
    int pos = -1;

    if (itemID == -1L) {
      return pos;
    }

    final Cursor cursor = mCursorAdapter.getCursor();
    if (cursor.moveToFirst()) {
      final int colIndex = cursor.getColumnIndex(ClipsContract.Clip._ID);
      while (!cursor.isAfterLast()) {
        if (cursor.getLong(colIndex) == itemID) {
          pos = cursor.getPosition();
          break;
        }
        cursor.moveToNext();
      }
    }

    return pos;
  }

  int getSelectedPos() {
    return mSelectedPos;
  }

  void setSelectedPos(int position) {
    if (mSelectedPos == position) {
      return;
    }

    if (position < 0) {
      mSelectedPos = -1;
      mSelectedItemID = -1L;
    } else {
      notifyItemChanged(mSelectedPos);
      mSelectedPos = position;
      notifyItemChanged(mSelectedPos);
    }
  }

  long getSelectedItemID() {
    return mSelectedItemID;
  }

  void setSelectedItemID(long itemID) {
    mSelectedItemID = itemID;
    setSelectedPos(getPosFromItemID(mSelectedItemID));
  }

  /**
   * Color the Vector Drawables based on theme and fav state
   * @param holder ClipViewHolder
   */
  private void tintIcons(ClipViewHolder holder) {
    final int color;
    final int drawableFav;
    final int colorFav;

    if (Prefs.INST(mContext).isLightTheme()) {
      color = android.R.color.primary_text_light;
    } else {
      color = android.R.color.primary_text_dark;
    }

    if (holder.favCheckBox.isChecked()) {
      drawableFav = R.drawable.ic_favorite_black_24dp;
      colorFav = R.color.red_500_translucent;
    } else {
      drawableFav = R.drawable.ic_favorite_border_black_24dp;
      colorFav = color;
    }

    DrawableHelper
      .withContext(mActivity)
      .withColor(color)
      .withDrawable(R.drawable.ic_content_copy_black_24dp)
      .tint()
      .applyTo(holder.copyButton);

    DrawableHelper
      .withContext(mActivity)
      .withColor(color)
      .withDrawable(R.drawable.ic_label_outline)
      .tint()
      .applyTo(holder.labelButton);

    DrawableHelper
      .withContext(mActivity)
      .withColor(colorFav)
      .withDrawable(drawableFav)
      .tint()
      .applyToDrawableLeft(holder.favCheckBox);
  }

  /** ViewHolder inner class used to display the info. in the RecyclerView. */
  static class ClipViewHolder extends RecyclerViewCursorViewHolder {
    final RelativeLayout clipBackground;
    final RelativeLayout clipForeground;
    final CheckBox favCheckBox;
    final TextView dateText;
    final ImageButton copyButton;
    final ImageButton labelButton;
    final TextView clipText;
    ClipItem clipItem;
    long itemID;

    ClipViewHolder(View view) {
      super(view);

      clipBackground = view.findViewById(R.id.clipBackground);
      clipForeground = view.findViewById(R.id.clipForeground);
      favCheckBox = view.findViewById(R.id.favCheckBox);
      dateText = view.findViewById(R.id.dateText);
      clipText = view.findViewById(R.id.clipText);
      copyButton = view.findViewById(R.id.copyButton);
      labelButton = view.findViewById(R.id.labelButton);
      clipItem = null;
      itemID = -1L;

      itemView.setTag(this);
      favCheckBox.setTag(this);
      copyButton.setTag(this);
      labelButton.setTag(this);
    }

    @Override
    public void bindCursor(final Cursor cursor) {
      clipItem = new ClipItem(clipText.getContext(), cursor);
      itemID = cursor.getLong(cursor.getColumnIndex(ClipsContract.Clip._ID));
      clipText.setText(clipItem.getText());
      favCheckBox.setChecked(clipItem.isFav());

      long time = clipItem.getTime();
      final CharSequence value =
        AppUtils
          .getRelativeDisplayTime(clipText.getContext(), clipItem.getDate());
      dateText.setText(value);
      dateText.setTag(time);
    }
  }
}
