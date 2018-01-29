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
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.databinding.LabelEditRowBinding;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.ui.base.BaseBindingAdapter;
import com.weebly.opus1269.clipman.ui.base.BaseViewHolder;
import com.weebly.opus1269.clipman.ui.base.VHAdapterFactory;
import com.weebly.opus1269.clipman.viewmodel.LabelViewModel;
import com.weebly.opus1269.clipman.ui.base.VMAdapterFactory;

/** Bridge between the RecyclerView and the database */
class LabelsEditAdapter extends BaseBindingAdapter<LabelEntity,
  LabelEditRowBinding, LabelHandlers, LabelViewModel,
  LabelsEditAdapter.LabelViewHolder> {

  LabelsEditAdapter(LifecycleOwner owner, LabelHandlers handlers) {
    super(new LabelViewHolderFactory(), new LabelViewModelFactory(),
      R.layout.label_edit_row, owner, handlers);
  }

  @Override
  public void onBindViewHolder(final LabelViewHolder holder, int position) {
    super.onBindViewHolder(holder, position);

    final LabelViewModel vm = holder.vm;

    // observe error
    vm.getErrorMsg().observe(mLifecycleOwner, errorMsg -> {
      if (errorMsg != null) {
        vm.resetName();
        AppUtils.showMessage(holder.itemView.getContext(), null, errorMsg.msg);
      }
    });
  }

  /** Factory to create an instance of our ViewHolder */
  static class LabelViewHolderFactory implements
    VHAdapterFactory<LabelViewHolder, LabelEditRowBinding> {

    @Override
    public LabelViewHolder create(LabelEditRowBinding binding) {
      return new LabelViewHolder(binding);
    }
  }

  /** Factory to create an instance of our ViewModel */
  static class LabelViewModelFactory implements
    VMAdapterFactory<LabelViewModel, LabelEntity> {

    @Override
    public LabelViewModel create(LabelEntity item) {
      return new LabelViewModel(App.INST(), item);
    }
  }

  /** Our ViewHolder */
  static class LabelViewHolder extends
    BaseViewHolder<LabelEditRowBinding, LabelViewModel, LabelHandlers> {

    LabelViewHolder(LabelEditRowBinding binding) {
      super(binding);
    }

    /** Bind the data */
    public void bind(LifecycleOwner owner, LabelViewModel vm,
                     LabelHandlers handlers) {
      super.bind(owner, vm, handlers);
    }
  }
}
