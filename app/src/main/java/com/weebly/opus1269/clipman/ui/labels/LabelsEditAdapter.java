/*
 *
 * Copyright 2016 Michael A Updike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorViewHolder;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.ClipContract;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/**
 * Bridge between the RecyclerView and the DB
 */
class LabelsEditAdapter extends RecyclerViewCursorAdapter<LabelsEditAdapter.LabelViewHolder> {

  private final Activity mActivity;

  LabelsEditAdapter(Activity activity) {
    super(activity);

    mActivity = activity;

    // needed to allow animations to run
    setHasStableIds(true);

    setupCursorAdapter(null, 0, R.layout.label_edit_row, false);
  }

  @Override
  public LabelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final Context context = parent.getContext();
    final LayoutInflater inflater = LayoutInflater.from(context);

    // Inflate the custom layout
    final View view = inflater.inflate(R.layout.label_edit_row, parent, false);

    // Return a new holder instance
    return new LabelViewHolder(view);
  }

  @Override
  public void onBindViewHolder(LabelViewHolder holder, int position) {
    // Move cursor to this position
    mCursorAdapter.getCursor().moveToPosition(position);

    // Set the ViewHolder
    setViewHolder(holder);

    // Bind this view
    mCursorAdapter.bindView(null, mContext, mCursorAdapter.getCursor());

    // color the icons
    tintIcons(holder);

    // Get the data model from the holder
    final Label label = holder.label;

    final EditText labelEditText = holder.labelEditText;
    labelEditText.setText(label.getName());
    holder.deleteButton.setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          // TODO remove from database
        }
      }
    );
  }

  @Override
  // needed to allow animations to run
  public long getItemId(int position) {
    return mCursorAdapter.getItemId(position);
  }

  /**
   * Color the Vector Drawables based on theme
   * @param holder LabelViewHolder
   */
  private void tintIcons(LabelViewHolder holder) {
    final Context context = holder.labelImage.getContext();
    int color;

    if (Prefs.isLightTheme()) {
      color = R.color.deep_teal_500;
    } else {
      color = R.color.deep_teal_200;
    }
    DrawableHelper
      .withContext(context)
      .withColor(color)
      .withDrawable(R.drawable.ic_label)
      .tint()
      .applyTo(holder.labelImage);

    if (Prefs.isLightTheme()) {
      color = android.R.color.primary_text_light;
    } else {
      color = android.R.color.primary_text_dark;
    }
    DrawableHelper
      .withContext(context)
      .withColor(color)
      .withDrawable(R.drawable.ic_clear_black_24dp)
      .tint()
      .applyTo(holder.deleteButton);
  }

  static class LabelViewHolder extends RecyclerViewCursorViewHolder {
    final ImageView labelImage;
    final EditText labelEditText;
    final ImageButton deleteButton;
    Label label;
    long itemID;

    LabelViewHolder(View view) {
      super(view);

      labelImage = view.findViewById(R.id.labelImage);
      labelEditText = view.findViewById(R.id.labelText);
      deleteButton = view.findViewById(R.id.deleteButton);
    }

    @Override
    public void bindCursor(final Cursor cursor) {
      label = new Label(cursor);

      itemID = cursor.getLong(cursor.getColumnIndex(ClipContract.Label._ID));

      labelEditText.setText(label.getName());
    }
  }
}
