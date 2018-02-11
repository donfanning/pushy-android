/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
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
  LabelSelectRowBinding, LabelSelectHandlers, LabelViewModel,
  LabelsSelectAdapter.LabelViewHolder> {
  /** Our Actvity */
  private final LabelsSelectActivity mActivity;

  LabelsSelectAdapter(LabelsSelectActivity activity,
                      LabelSelectHandlers handlers) {
    super(new LabelViewHolderFactory(), new LabelViewModelFactory(),
      R.layout.label_select_row, activity, handlers);
    mActivity = activity;
  }

  @Override
  public void onBindViewHolder(final LabelViewHolder holder, int position) {
    super.onBindViewHolder(holder, position);

    if (mActivity.getVm().hasLabel(holder.binding.getVm().getNameSync())) {
      if (!holder.binding.checkBox.isChecked()) {
        holder.binding.checkBox.setChecked(true);
      }
    } else {
      if (holder.binding.checkBox.isChecked()) {
        holder.binding.checkBox.setChecked(false);
      }
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
    BaseViewHolder<LabelSelectRowBinding, LabelViewModel, LabelSelectHandlers> {

    LabelViewHolder(LabelSelectRowBinding binding) {
      super(binding);
    }

    /** Bind the data */
    public void bind(LifecycleOwner owner, LabelViewModel vm,
                     LabelSelectHandlers handlers) {
      super.bind(owner, vm, handlers);
    }
  }
}
