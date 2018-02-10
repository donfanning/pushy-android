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
import com.weebly.opus1269.clipman.databinding.ClipRowBinding;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.ui.base.BaseBindingAdapter;
import com.weebly.opus1269.clipman.ui.base.BaseViewHolder;
import com.weebly.opus1269.clipman.ui.base.VHAdapterFactory;
import com.weebly.opus1269.clipman.ui.base.VMAdapterFactory;
import com.weebly.opus1269.clipman.viewmodel.ClipViewModel;

import java.util.List;

/** Bridge between the Clip RecyclerView and the Clip class */
class ClipAdapter extends BaseBindingAdapter<Clip, ClipRowBinding,
  MainHandlers, ClipViewModel, ClipAdapter.ClipViewHolder> {
  /** Our Activity */
  @NonNull
  private final MainActivity mActivity;

  /** Our List */
  @Nullable
  private List<Clip> mList;

  ClipAdapter(@NonNull MainActivity activity, MainHandlers handlers) {
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
        if (holder.itemView.isSelected()) {
          holder.itemView.setSelected(false);
        }
      }
    } else {
      // should never be selected in singlepane
      if (holder.itemView.isSelected()) {
        holder.itemView.setSelected(false);
      }
    }
  }

  @Override
  public void setList(List<Clip> list) {
    if (AppUtils.isDualPane(App.INST())) {
      // move selection in current list to new list
      final Clip selClip = mActivity.getSelectedClipSync();
      final int pos = getSelectedPos(mList, selClip);
      final int newPos = getSelectedPos(list, selClip);

      if (pos == newPos) {
        super.setList(list);
      } else {
        setSelected(selClip, false);
        super.setList(list);
        setSelected(selClip, true);
      }
    } else {
      super.setList(list);
    }
    mList = list;
  }

  public void changeSelection(@Nullable Clip lastSel,
                              @Nullable Clip curSel) {
    if (AppUtils.isDualPane(App.INST()) && idChanged(lastSel, curSel)) {
      setSelected(lastSel, false);
      setSelected(curSel, true);
    }
  }

  @Nullable
  private ClipViewHolder getHolder(@Nullable Clip clip) {
    ClipViewHolder ret = null;
    if (clip != null) {
      ret = (ClipViewHolder) mActivity.getRecyclerView()
        .findViewHolderForItemId(clip.getId());
    }
    return ret;
  }

  private boolean idChanged(@Nullable Clip clip1,
                            @Nullable Clip clip2) {
    return !(clip1 != null && clip2 != null && clip1.getId() == clip2.getId());
  }

  private void setSelected(@Nullable Clip clip, boolean isSelected) {
    final ClipViewHolder holder = getHolder(clip);
    if (holder != null && holder.itemView.isSelected() != isSelected) {
      holder.itemView.setSelected(isSelected);
    }
  }
  //public void changeSelection(@Nullable Clip lastSelected) {
  //  if (AppUtils.isDualPane(App.INST())) {
  //    final int pos = getSelectedPos(mList, lastSelected);
  //    final int newPos = getSelectedPos(mList, mActivity
  // .getSelectedClipSync());
  //
  //    if (pos != newPos) {
  //      if (pos != -1) {
  //        notifyItemChanged(pos);
  //      }
  //      if (newPos != -1) {
  //        notifyItemChanged(newPos);
  //      }
  //    }
  //  }
  //}

  private int getSelectedPos(@Nullable List<Clip> clips,
                             @Nullable Clip selClip) {
    int pos = -1;
    if ((selClip != null) && !AppUtils.isEmpty(clips)) {
      pos = clips.indexOf(selClip);
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
    VMAdapterFactory<ClipViewModel, Clip> {

    @Override
    public ClipViewModel create(Clip item) {
      return new ClipViewModel(App.INST(), item);
    }
  }

  /** Our ViewHolder */
  static class ClipViewHolder extends
    BaseViewHolder<ClipRowBinding, ClipViewModel, MainHandlers> {

    ClipViewHolder(ClipRowBinding binding) {
      super(binding);
    }

    /** Bind the Clip */
    public void bind(LifecycleOwner owner, ClipViewModel vm,
                     MainHandlers handlers) {
      super.bind(owner, vm, handlers);
    }
  }
}
