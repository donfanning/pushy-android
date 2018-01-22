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

import android.arch.lifecycle.LifecycleOwner;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.DiffCallback;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.databinding.LabelEditRowBinding;
import com.weebly.opus1269.clipman.db.entity.BaseEntity;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.ui.base.BaseBindingAdapter;
import com.weebly.opus1269.clipman.viewmodel.LabelViewModel;

import java.util.List;

/** Bridge between the RecyclerView and the database */
class LabelsEditAdapter extends
  BaseBindingAdapter<LabelEntity, LabelsEditAdapter.LabelViewHolder> {

  LabelsEditAdapter(LifecycleOwner owner, LabelHandlers handlers) {
    super(R.layout.label_edit_row, owner, handlers);
  }

  @Override
  public LabelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    final LabelEditRowBinding binding = DataBindingUtil
      .inflate(inflater, mlayoutId, parent, false);

    return new LabelsEditAdapter.LabelViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(final LabelViewHolder holder, int position) {
    //final LabelEntity label = mList.get(position);
    final LabelEntity label = getItem(position);
    final String originalName = label.getName();
    final LabelViewModel viewModel = new LabelViewModel(App.INST(), label);
    holder.bind(mLifecycleOwner, viewModel, (LabelHandlers)mHandlers);

    final EditText labelText = holder.binding.labelText;
    labelText.setOnFocusChangeListener((view, hasFocus) -> {
      if (!hasFocus) {
        String text = labelText.getText().toString();
        text = text.trim();
        if (text.length() > 0) {
          if (!text.equals(originalName)) {
            // update label
            viewModel.setName(text);
          } else {
            // reset to orginal value
            viewModel.getName().setValue(originalName);
          }
        } else {
          // reset to orginal value
          viewModel.getName().setValue(originalName);
        }
      }
    });
  }

  static class LabelViewHolder extends RecyclerView.ViewHolder {
    private final LabelEditRowBinding binding;

    LabelViewHolder(@NonNull LabelEditRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    /** Bind the data */
    void bind(LifecycleOwner owner, LabelViewModel vm, LabelHandlers handlers) {
      binding.setLifecycleOwner(owner);
      binding.setVm(vm);
      binding.setHandlers(handlers);
      binding.executePendingBindings();
    }
  }
}
