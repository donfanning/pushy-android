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

import android.arch.lifecycle.MutableLiveData;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.databinding.LabelEditRowBinding;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.LabelNew;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;
import com.weebly.opus1269.clipman.viewmodel.LabelViewModel;

import java.util.Arrays;
import java.util.List;

/** Bridge between the RecyclerView and the database */
class LabelsEditAdapter extends
  RecyclerView.Adapter<LabelsEditAdapter.LabelViewHolder> {
  /** Class identifier */
  protected final String TAG = this.getClass().getSimpleName();

  /** Our activity */
  private final BaseActivity mActivity;

  /** Our event handlers */
  private final LabelHandlers mHandlers;

  /** Our list */
  private List<LabelEntity> mList;

  LabelsEditAdapter(BaseActivity activity, LabelHandlers handlers) {
    super();

    mActivity = activity;
    mHandlers = handlers;
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
    final LabelEntity label = mList.get(position);
    final String originalName = label.getName();
    final LabelViewModel viewModel = new LabelViewModel(App.INST(), label);
    holder.bind(mActivity, viewModel, mHandlers);

    //final boolean enabled = originalName.equals(label.getName());
    //DrawableHelper.setImageViewEnabled(holder.binding.deleteButton, enabled);
    //
    final EditText labelText = holder.binding.labelText;
    //labelText.addTextChangedListener(new TextWatcher() {
    //  @Override
    //  public void beforeTextChanged(CharSequence text, int i, int i1, int i2) {
    //    // noop
    //  }
    //
    //  @Override
    //  public void onTextChanged(CharSequence text, int i, int i1, int i2) {
    //    final boolean enabled = text.toString().equals(originalName);
    //    DrawableHelper.setImageViewEnabled(holder.binding.deleteButton, enabled);
    //  }
    //
    //  @Override
    //  public void afterTextChanged(Editable editable) {
    //    // noop
    //  }
    //});

    labelText.setOnFocusChangeListener((view, hasFocus) -> {
      if (!hasFocus) {
        String text = labelText.getText().toString();
        text = text.trim();
        if (text.length() > 0) {
          if (!text.equals(originalName)) {
            // update label
            //Log.logD(TAG, "name: " + viewModel.name.getValue());
            //Log.logD(TAG, "name: " + viewModel.getLabel().getValue().getName());
            viewModel.setName(text);
            //viewModel.getLabel().getValue().setName(text);
          } else {
            // reset to orginal value
            //labelText.setText(originalName);
            //Log.logD(TAG, "name: " + viewModel.getLabel().getValue().getName());
            viewModel.getName().setValue(originalName);
            //viewModel.name.setValue(originalName);
            //Log.logD(TAG, "name: " + viewModel.getLabel().getValue().getName());
            //viewModel.setName(originalName);
          }
        } else {
          // reset to orginal value
          //labelText.setText(originalName);
          viewModel.setName(originalName);
        }
      }
    });
  }

  @Override
  public int getItemCount() {return AppUtils.size(mList);}

  public void setList(@Nullable List<LabelEntity> list) {
    // small list, just update it all
    Log.logD(TAG, "setList");
    mList = list;
    notifyDataSetChanged();
  }

  static class LabelViewHolder extends RecyclerView.ViewHolder {
    private final LabelEditRowBinding binding;

    LabelViewHolder(@NonNull LabelEditRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      tintIcons();
    }

    /** Bind the Label */
    void bind(BaseActivity activity, LabelViewModel viewModel,
              LabelHandlers handlers) {
      binding.setLifecycleOwner(activity);
      binding.setVm(viewModel);
      binding.setHandlers(handlers);
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
