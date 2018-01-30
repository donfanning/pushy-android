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

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
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

  /** our clips */
  private List<ClipEntity> mClipList = null;

  /** The currently selected Clip */
  private ClipEntity mSelectedClip = null;

  /** The currently selected position in the list */
  private int mSelectedPos = 0;

  ClipAdapter(LifecycleOwner owner, ClipHandlers handlers) {
    super(new ClipViewHolderFactory(), new ClipViewModelFactory(),
      R.layout.clip_row, owner, handlers);
  }

  @Override
  public void onBindViewHolder(ClipViewHolder holder, int position) {
    super.onBindViewHolder(holder, position);

    if (AppUtils.isDualPane(App.INST())) {
      // set selected state of the view
      if (getSelectedPos() == position) {
        if (!holder.itemView.isSelected()) {
          holder.itemView.setSelected(true);
        }
      } else {
        holder.itemView.setSelected(false);
      }
    }
  }

  @Override
  public void setList(List<ClipEntity> list) {
    super.setList(list);
    mClipList = list;
  }

  public int getSelectedPos() {
    return mSelectedPos;
  }

  void setSelectedPos(int position) {
    if (mSelectedPos == position) {
      return;
    }

    if (position < 0) {
      mSelectedPos = -1;
      mSelectedClip = null;
    } else {
      notifyItemChanged(mSelectedPos);
      mSelectedPos = position;
      notifyItemChanged(mSelectedPos);
    }
  }

  void setSelectedClip(ClipEntity clip) {
    mSelectedClip = clip;
    int pos = -1;
    if (mClipList != null) {
      for (int i = 0; i < mClipList.size(); i++) {
        if (mClipList.get(i).getText().equals(clip.getText())) {
          pos = i;
          break;
        }
      }
    }
    setSelectedPos(pos);
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
