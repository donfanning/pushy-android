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
import android.content.DialogInterface;
import android.database.Cursor;
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
import com.weebly.opus1269.clipman.db.ClipsContract;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/** Bridge between the RecyclerView and the database */
class LabelsEditAdapter extends
  RecyclerViewCursorAdapter<LabelsEditAdapter.LabelViewHolder> implements
  DialogInterface.OnClickListener {

  /** Our activity */
  private final BaseActivity mActivity;

  /** Our delete dialog */
  private AlertDialog mDialog;

  /** Label that may be deleted */
  private Label mDeleteLabel;

  LabelsEditAdapter(BaseActivity activity) {
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
      public void beforeTextChanged(CharSequence text, int i, int i1, int i2) {}

      @Override
      public void onTextChanged(CharSequence text, int i, int i1, int i2) {}

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
        }
      }
    });

    labelEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
          String text = labelEditText.getText().toString();
          text = text.trim();
          if (text.length() > 0) {
            if (!text.equals(holder.label.getName())) {
              holder.label.setName(text);
            }
            labelEditText.setText(holder.label.getName());
            DrawableHelper.setImageViewEnabled(holder.deleteButton, true);
          } else {
            // reset to orginal value
            labelEditText.setText(holder.label.getName());
            DrawableHelper.setImageViewEnabled(holder.deleteButton, true);
          }
        }
      }
    });

    holder.deleteButton.setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Analytics.INST.imageClick(mActivity.getTAG(), "deleteLabel");
          mDeleteLabel = holder.label;
          showDeleteDialog();
        }
      }
    );
  }

  /** needed to allow animations to run */
  @Override
  public long getItemId(int position) {
    return mCursorAdapter.getItemId(position);
  }

  @Override
  public void onClick(DialogInterface dialogInterface, int which) {
    if ((which == DialogInterface.BUTTON_POSITIVE) && (mDeleteLabel != null)) {
      // delete it
      mDeleteLabel.delete();
      mDeleteLabel = null;
      Analytics.INST.buttonClick(mActivity.getTAG(), mDialog.getButton(which));
    }
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
      .withDrawable(R.drawable.ic_clear)
      .tint()
      .applyTo(holder.deleteButton);
  }

  /** Display {@link AlertDialog} on {@link Label} delete */
  private void showDeleteDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

    builder
      .setMessage(R.string.label_delete_dialog_message)
      .setTitle(R.string.label_delete_dialog_title)
      .setPositiveButton(R.string.button_delete, this)
      .setNegativeButton(R.string.button_cancel, this);

    mDialog = builder.create();
    mDialog.show();
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

      itemID = cursor.getLong(cursor.getColumnIndex(ClipsContract.Label._ID));

      labelEditText.setText(label.getName());
    }
  }
}
