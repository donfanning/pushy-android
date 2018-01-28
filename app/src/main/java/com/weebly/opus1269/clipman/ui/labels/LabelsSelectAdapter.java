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

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.databinding.LabelSelectRowBinding;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;
import com.weebly.opus1269.clipman.viewmodel.LabelViewModel;

import java.util.Collections;
import java.util.List;

/** Bridge between the RecyclerView and the database */
class LabelsSelectAdapter extends
  RecyclerView.Adapter<LabelsSelectAdapter.LabelViewHolder> {
  /** Our activity */
  private final LabelsSelectActivity mActivity;

  /** Activity TAG */
  private final String TAG;

  /** Our list */
  private List<LabelEntity> mList;

  LabelsSelectAdapter(LabelsSelectActivity activity) {
    super();

    mActivity = activity;
    TAG = activity.getTAG();
  }

  @Override
  public LabelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LabelSelectRowBinding binding = DataBindingUtil
      .inflate(LayoutInflater.from(parent.getContext()), R.layout.label_select_row,
        parent, false);

    return new LabelViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(final LabelViewHolder holder, int position) {
    final LabelViewModel vm =
      new LabelViewModel(App.INST(), mList.get(position));
    //TODO holder.bind(vm, mHandlers);
    holder.bind(vm);
    // color the icons
    tintIcons(holder);

    // Get the data model from the holder
    final Label label = holder.label;

    // set checked state
    final boolean checked = mActivity.getClip().hasLabel(label);
    holder.checkBox.setChecked(checked);

    holder.labelText.setText(label.getName());

    holder.labelRow.setOnClickListener(v -> {
        Analytics.INST(v.getContext()).click(TAG, "selectLabel");
        holder.checkBox.toggle();
        //TODO addOrRemoveLabel(holder.checkBox.isChecked(), label);
      }
    );

    holder.checkBox.setOnClickListener(v -> {
        final boolean checked1 = holder.checkBox.isChecked();
        //TODO addOrRemoveLabel(checked1, label);
        Analytics.INST(v.getContext())
          .checkBoxClick(TAG, "selectLabel: " + checked1);
      }
    );
  }

  @Override
  public int getItemCount() {return AppUtils.size(mList);}

  ///**
  // * Add or remove a {@link Label} to our {@link ClipItem}
  // * @param checked if true, add
  // * @param label   label to add or remove
  // */
  //private void addOrRemoveLabel(boolean checked, Label label) {
  //  final ClipItem clipItem = mActivity.getClip();
  //  if (checked) {
  //    clipItem.addLabel(mContext, label);
  //  } else {
  //    clipItem.removeLabel(mContext, label);
  //  }
  //}

  public void setList(@Nullable List<LabelEntity> list) {
    // small list, just update it all
    mList = list;
    notifyDataSetChanged();
  }

  /**
   * Color the Vector Drawables based on theme
   * @param holder LabelViewHolder
   */
  private void tintIcons(LabelViewHolder holder) {
    final List<ImageView> list = Collections.singletonList(holder.labelImage);
    DrawableHelper.tintAccentColor(holder.labelImage.getContext(), list);
  }

  static class LabelViewHolder extends RecyclerView.ViewHolder {
    private final LabelSelectRowBinding binding;
    final RelativeLayout labelRow;
    final ImageView labelImage;
    final TextView labelText;
    final CheckBox checkBox;
    Label label;

    LabelViewHolder(@NonNull LabelSelectRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      labelRow = binding.labelRow;
      labelImage = binding.labelImage;
      labelText = binding.labelText;
      checkBox = binding.checkBox;
    }

    /** Bind the File */
    void bind(LabelViewModel vm) {
      binding.setVm(vm);
      //TODO binding.setHandlers(handlers);
      binding.executePendingBindings();
    }
  }
}
