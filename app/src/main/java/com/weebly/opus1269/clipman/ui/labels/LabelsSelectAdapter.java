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

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.androidessence.recyclerviewcursoradapter
  .RecyclerViewCursorViewHolder;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/**
 * Bridge between the RecyclerView and the DB
 */
class LabelsSelectAdapter extends
  RecyclerViewCursorAdapter<LabelsSelectAdapter.LabelViewHolder> {

  private final LabelsSelectActivity mActivity;

  LabelsSelectAdapter(LabelsSelectActivity activity) {
    super(activity);

    mActivity = activity;

    // needed to allow animations to run
    setHasStableIds(true);

    setupCursorAdapter(null, 0, R.layout.label_select_row, false);
  }

  @Override
  public LabelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final Context context = parent.getContext();
    final LayoutInflater inflater = LayoutInflater.from(context);

    // Inflate the custom layout
    final View view = inflater.inflate(R.layout.label_select_row, parent,
      false);

    // Return a new holder instance
    return new LabelViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final LabelViewHolder holder, int position) {
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

    // set checked state
    final boolean checked = mActivity.getClipItem().hasLabel(label);
    holder.checkBox.setChecked(checked);

    final TextView textView = holder.labelText;
    textView.setText(label.getName());

    holder.labelRow.setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Analytics.INST(v.getContext())
            .click(mActivity.getTAG(), "selectLabel");
          holder.checkBox.toggle();
          final boolean checked = holder.checkBox.isChecked();
          addOrRemoveLabel(checked, label);
        }
      }
    );

    holder.checkBox.setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          final boolean checked = holder.checkBox.isChecked();
          addOrRemoveLabel(checked, label);
          Analytics.INST(v.getContext())
            .checkBoxClick(mActivity.getTAG(), "selectLabel: " + checked);
        }
      }
    );

  }

  /** needed to allow animations to run */
  @Override
  public long getItemId(int position) {
    return mCursorAdapter.getItemId(position);
  }

  /**
   * Add or remove a {@link Label} to our {@link ClipItem}
   * @param checked if true, add
   * @param label   label to add or remove
   */
  private void addOrRemoveLabel(boolean checked, Label label) {
    final ClipItem clipItem = mActivity.getClipItem();
    if (checked) {
      clipItem.addLabel(label);
    } else {
      clipItem.removeLabel(label);
    }
  }

  /**
   * Color the Vector Drawables based on theme
   * @param holder LabelViewHolder
   */
  private void tintIcons(LabelViewHolder holder) {
    final Context context = holder.labelImage.getContext();
    int color;

    if (Prefs.INST(mContext).isLightTheme()) {
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
  }

  static class LabelViewHolder extends RecyclerViewCursorViewHolder {
    final RelativeLayout labelRow;
    final ImageView labelImage;
    final TextView labelText;
    final CheckBox checkBox;
    Label label;

    LabelViewHolder(View view) {
      super(view);

      labelRow = view.findViewById(R.id.labelRow);
      labelImage = view.findViewById(R.id.labelImage);
      labelText = view.findViewById(R.id.labelText);
      checkBox = view.findViewById(R.id.checkBox);
    }

    @Override
    public void bindCursor(final Cursor cursor) {
      label = new Label(cursor);
      labelText.setText(label.getName());
    }
  }
}
