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

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.databinding.LabelSelectRowBinding;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.ui.base.BaseBindingAdapter;
import com.weebly.opus1269.clipman.ui.base.BaseViewHolder;
import com.weebly.opus1269.clipman.ui.base.VHAdapterFactory;
import com.weebly.opus1269.clipman.viewmodel.LabelViewModel;
import com.weebly.opus1269.clipman.ui.base.VMAdapterFactory;

/** Bridge between the RecyclerView and the database */
class LabelsSelectAdapter extends BaseBindingAdapter<Label,
  LabelSelectRowBinding, LabelHandlers, LabelViewModel,
  LabelsSelectAdapter.LabelViewHolder> {
  private final LabelsSelectActivity mActivity;

  LabelsSelectAdapter(LabelsSelectActivity activity, LabelHandlers handlers) {
    super(new LabelViewHolderFactory(), new LabelViewModelFactory(),
      R.layout.label_select_row, activity, handlers);
    mActivity = activity;
  }

  @Override
  public void onBindViewHolder(final LabelViewHolder holder, int position) {
    super.onBindViewHolder(holder, position);

    if(mActivity.hasLabel(holder.binding.labelText.getText().toString())) {
      holder.binding.checkBox.setChecked(true);
    } else {
      holder.binding.checkBox.setChecked(false);
    }
  }

  /** Factory to create an instance of our ViewHolder */
  static class LabelViewHolderFactory implements
    VHAdapterFactory<LabelViewHolder, LabelSelectRowBinding> {

    @Override
    public LabelViewHolder create(LabelSelectRowBinding binding) {
      return new LabelViewHolder(binding);
    }
  }

  /** Factory to create an instance of our ViewModel */
  static class LabelViewModelFactory implements
    VMAdapterFactory<LabelViewModel, Label> {

    @Override
    public LabelViewModel create(Label item) {
      return new LabelViewModel(App.INST(), item);
    }
  }

  /** Our ViewHolder */
  static class LabelViewHolder extends
    BaseViewHolder<LabelSelectRowBinding, LabelViewModel, LabelHandlers> {

    LabelViewHolder(LabelSelectRowBinding binding) {
      super(binding);
    }

    /** Bind the data */
    public void bind(LifecycleOwner owner, LabelViewModel vm,
                     LabelHandlers handlers) {
      super.bind(owner, vm, handlers);
    }
  }
}
