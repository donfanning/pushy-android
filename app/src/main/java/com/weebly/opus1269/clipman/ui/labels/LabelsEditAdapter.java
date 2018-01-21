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
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.databinding.LabelEditRowBinding;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;
import com.weebly.opus1269.clipman.viewmodel.LabelViewModel;

import java.util.Arrays;
import java.util.List;

/** Bridge between the RecyclerView and the database */
class LabelsEditAdapter extends
  RecyclerView.Adapter<LabelsEditAdapter.LabelViewHolder> {
  /** Our activity */
  private final BaseActivity mActivity;

  /** Activity TAG */
  private final String TAG;

  /** Our list */
  private List<LabelEntity> mList;

  /** Label that may be deleted */
  @Nullable
  private LabelEntity mDeleteLabel;

  LabelsEditAdapter(BaseActivity activity) {
    super();

    mActivity = activity;
    TAG = mActivity.getTAG();
  }

  @Override
  public LabelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LabelEditRowBinding binding = DataBindingUtil
      .inflate(LayoutInflater.from(parent.getContext()), R.layout.label_edit_row,
        parent, false);

    return new LabelsEditAdapter.LabelViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(final LabelViewHolder holder, int position) {
    final LabelViewModel vm =
      new LabelViewModel(App.INST(), mList.get(position));
    //TODO holder.bind(vm, mHandlers);
    holder.bind(vm);

    final EditText labelEditText = holder.labelEditText;
    labelEditText.setText(vm.label.getName());
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
          holder.labelEditText.setText(vm.label.getName());
          DrawableHelper.setImageViewEnabled(holder.deleteButton, true);
        } else if (!text.equals(vm.label.getName())) {
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
          if (!text.equals(vm.label.getName())) {
            vm.label.setName(text);
          }
          labelEditText.setText(vm.label.getName());
          DrawableHelper.setImageViewEnabled(holder.deleteButton, true);
        } else {
          // reset to orginal value
          labelEditText.setText(vm.label.getName());
          DrawableHelper.setImageViewEnabled(holder.deleteButton, true);
        }
      }
    });

    holder.deleteButton.setOnClickListener(v -> {
        Analytics.INST(v.getContext()).imageClick(TAG, "deleteLabel");
        mDeleteLabel = vm.label;
        showDeleteDialog();
      }
    );
  }

  @Override
  public int getItemCount() {return AppUtils.size(mList);}

  public void setList(@Nullable List<LabelEntity> list) {
    // small list, just update it all
    mList = list;
    notifyDataSetChanged();
  }

  /** Display {@link AlertDialog} on {@link Label} delete */
  private void showDeleteDialog() {
    final AlertDialog dialog = new AlertDialog.Builder(mActivity)
      .setMessage(R.string.label_delete_dialog_message)
      .setTitle(R.string.label_delete_dialog_title)
      .setPositiveButton(R.string.button_delete, (dialogInterface, i) -> {
        if ((mDeleteLabel != null)) {
          //mDeleteLabel.delete(mContext);
          mDeleteLabel = null;
          Analytics.INST(mActivity.getApplicationContext()).buttonClick(TAG, "deleteLabel");
        }
      })
      .setNegativeButton(R.string.button_cancel, null)
      .create();

    dialog.show();
  }

  static class LabelViewHolder extends RecyclerView.ViewHolder {
    private final LabelEditRowBinding binding;
    final ImageView labelImage;
    final EditText labelEditText;
    final ImageButton deleteButton;

    LabelViewHolder(@NonNull LabelEditRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;

      labelImage = binding.labelImage;
      labelEditText = binding.labelText;
      deleteButton = binding.deleteButton;

      tintIcons();
    }

    /** Bind the Label */
    void bind(LabelViewModel vm) {
      binding.setVm(vm);
      //TODO binding.setHandlers(handlers);
      binding.executePendingBindings();
    }

    /** Color the Vector Drawables based on theme */
    private void tintIcons() {
      final List<ImageView> list = Arrays.asList(
        binding.labelImage, binding.deleteButton
      );
      DrawableHelper.tintAccentColor(binding.labelImage.getContext(), list);
    }
  }
}
