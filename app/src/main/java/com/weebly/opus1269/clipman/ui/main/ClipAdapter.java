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

package com.weebly.opus1269.clipman.ui.main;

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.databinding.ClipRowBinding;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.ui.base.BaseBindingAdapter;
import com.weebly.opus1269.clipman.ui.base.BaseViewHolder;
import com.weebly.opus1269.clipman.ui.base.VHAdapterFactory;
import com.weebly.opus1269.clipman.ui.base.VMAdapterFactory;
import com.weebly.opus1269.clipman.viewmodel.ClipViewModel;

import java.util.List;

/** Bridge between the Devices RecyclerView and the Devices class */
class ClipAdapter extends BaseBindingAdapter<ClipEntity, ClipRowBinding,
  ClipHandlers, ClipViewModel, ClipAdapter.ClipViewHolder> {
  /** Our Activity */
  @NonNull
  private final MainActivity mActivity;

  /** Our List */
  @Nullable
  private List<ClipEntity> mList;

  ClipAdapter(@NonNull MainActivity activity, ClipHandlers handlers) {
    super(new ClipViewHolderFactory(), new ClipViewModelFactory(),
      R.layout.clip_row, activity, handlers);
    mActivity = activity;
  }

  @Override
  public void onBindViewHolder(ClipViewHolder holder, int position) {
    super.onBindViewHolder(holder, position);

    if (AppUtils.isDualPane(App.INST())) {
      // set selected state of the view
      if (mActivity.getSelectedClipId() == getItemId(position)) {
        if (!holder.itemView.isSelected()) {
          holder.itemView.setSelected(true);
        }
      } else {
        holder.itemView.setSelected(false);
      }
    } else {
      // should never be selected in singlepane
      if (holder.itemView.isSelected()) {
        holder.itemView.setSelected(false);
      }
    }
  }

  @Override
  public void setList(List<ClipEntity> list) {
    deselect();
    super.setList(list);
    mList = list;
    select();
  }

  public void changeSelection(@Nullable ClipEntity lastSelected) {
    final int pos = getSelectedPos(mList, lastSelected);
    final int newPos = getSelectedPos(mList, mActivity.getSelectedClipSync());

    if (pos != newPos) {
      if (pos != -1) {
        notifyItemChanged(pos);
      }
      if (newPos != -1) {
        notifyItemChanged(newPos);
      }
    }
  }

  private void deselect() {
    if (AppUtils.isDualPane(App.INST())) {
      final ClipEntity selClip = mActivity.getSelectedClipSync();
      int pos = getSelectedPos(mList, selClip);
      if (pos != -1) {
        Log.logD(TAG, "deselected pos: " + pos);
        notifyItemChanged(pos);
      }
    }
  }

  private void select() {
    if (AppUtils.isDualPane(App.INST())) {
      final ClipEntity selClip = mActivity.getSelectedClipSync();
      int pos = getSelectedPos(mList, selClip);
      if (pos != -1) {
        Log.logD(TAG, "selected pos: " + pos);
        notifyItemChanged(pos);
      }
    }
  }

  private int getSelectedPos(@Nullable List<ClipEntity> clips,
                             @Nullable ClipEntity selClip) {
    int pos = -1;
    if ((selClip != null) && !AppUtils.isEmpty(clips)) {
      for (int i = 0; i < clips.size(); i++) {
        if (clips.get(i).getId() == selClip.getId()) {
          pos = i;
          break;
        }
      }
    }
    return pos;
  }


  /** Factory to create an instance of our ViewHolder */
  static class ClipViewHolderFactory implements
    VHAdapterFactory<ClipViewHolder, ClipRowBinding> {
    ClipViewHolderFactory() {}

    @Override
    public ClipViewHolder create(ClipRowBinding binding) {
      return new ClipViewHolder(binding);
    }
  }

  /** Factory to create an instance of our ViewModel */
  static class ClipViewModelFactory implements
    VMAdapterFactory<ClipViewModel, ClipEntity> {

    @Override
    public ClipViewModel create(ClipEntity item) {
      return new ClipViewModel(App.INST(), item);
    }
  }

  /** Our ViewHolder */
  static class ClipViewHolder extends
    BaseViewHolder<ClipRowBinding, ClipViewModel, ClipHandlers> {

    ClipViewHolder(ClipRowBinding binding) {
      super(binding);
    }

    /** Bind the Clip */
    public void bind(LifecycleOwner owner, ClipViewModel vm,
                     ClipHandlers handlers) {
      super.bind(owner, vm, handlers);
    }
  }
}
