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
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.androidessence.recyclerviewcursoradapter
  .RecyclerViewCursorViewHolder;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

import java.util.Arrays;
import java.util.List;

/** Bridge between the RecyclerView and the database */
class LabelsEditAdapter extends
  RecyclerViewCursorAdapter<LabelsEditAdapter.LabelViewHolder> {
  /** Our activity */
  private final BaseActivity mActivity;

  /** Activity TAG */
  private final String TAG;

  /** Label that may be deleted */
  @Nullable
  private Label mDeleteLabel;

  LabelsEditAdapter(BaseActivity activity) {
    super(activity);

    mActivity = activity;
    TAG = mActivity.getTAG();

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
  public void onBindViewHolder(final LabelViewHolder holder, int position) {
    // Move cursor to this position
    mCursorAdapter.getCursor().moveToPosition(position);

    // Set the ViewHolder
    setViewHolder(holder);

    // Bind this view
    mCursorAdapter.bindView(null, mContext, mCursorAdapter.getCursor());

    // color the icons
    tintIcons(holder);

    final EditText labelEditText = holder.labelEditText;
    labelEditText.setText(holder.label.getName());
    labelEditText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence text, int i, int i1, int i2) {
        // noop
      }

      @Override
      public void onTextChanged(CharSequence text, int i, int i1, int i2) {
        // noop
      }

      @Override
      public void afterTextChanged(Editable editable) {
        final String text = editable.toString();
        if (AppUtils.isWhitespace(text)) {
          // reset to current value
          holder.labelEditText.setText(holder.label.getName());
          DrawableHelper.setImageViewEnabled(holder.deleteButton, true);
        } else if (!text.equals(holder.label.getName())) {
          // text changed
          DrawableHelper.setImageViewEnabled(holder.deleteButton, false);
        } else {
          // original text
          DrawableHelper.setImageViewEnabled(holder.deleteButton, true);
        }
      }
    });

    labelEditText.setOnFocusChangeListener((view, hasFocus) -> {
      if (!hasFocus) {
        String text = labelEditText.getText().toString();
        text = text.trim();
        if (text.length() > 0) {
          if (!text.equals(holder.label.getName())) {
            holder.label.setName(mContext, text);
          }
          labelEditText.setText(holder.label.getName());
          DrawableHelper.setImageViewEnabled(holder.deleteButton, true);
        } else {
          // reset to orginal value
          labelEditText.setText(holder.label.getName());
          DrawableHelper.setImageViewEnabled(holder.deleteButton, true);
        }
      }
    });

    holder.deleteButton.setOnClickListener(v -> {
        Analytics.INST(v.getContext()).imageClick(TAG, "deleteLabel");
        mDeleteLabel = holder.label;
        showDeleteDialog();
      }
    );
  }

  /** needed to allow animations to run */
  @Override
  public long getItemId(int position) {
    return mCursorAdapter.getItemId(position);
  }

  /**
   * Color the Vector Drawables based on theme
   * @param holder LabelViewHolder
   */
  private void tintIcons(LabelViewHolder holder) {
    final List<ImageView> list = Arrays.asList(
      holder.labelImage, holder.deleteButton
    );
    DrawableHelper.tintAccentColor(holder.labelImage.getContext(), list);
  }

  /** Display {@link AlertDialog} on {@link Label} delete */
  private void showDeleteDialog() {
    final AlertDialog dialog = new AlertDialog.Builder(mActivity)
      .setMessage(R.string.label_delete_dialog_message)
      .setTitle(R.string.label_delete_dialog_title)
      .setPositiveButton(R.string.button_delete, (dialogInterface, i) -> {
        if ((mDeleteLabel != null)) {
          mDeleteLabel.delete(mContext);
          mDeleteLabel = null;
          Analytics.INST(mContext).buttonClick(TAG, "deleteLabel");
        }
      })
      .setNegativeButton(R.string.button_cancel, null)
      .create();

    dialog.show();
  }

  static class LabelViewHolder extends RecyclerViewCursorViewHolder {
    final ImageView labelImage;
    final EditText labelEditText;
    final ImageButton deleteButton;
    Label label;

    LabelViewHolder(View view) {
      super(view);

      labelImage = view.findViewById(R.id.labelImage);
      labelEditText = view.findViewById(R.id.labelText);
      deleteButton = view.findViewById(R.id.deleteButton);
    }

    @Override
    public void bindCursor(final Cursor cursor) {
      label = new Label(cursor);
      labelEditText.setText(label.getName());
    }
  }
}
