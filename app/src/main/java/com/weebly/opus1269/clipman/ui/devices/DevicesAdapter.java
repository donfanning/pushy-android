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

package com.weebly.opus1269.clipman.ui.devices;

import android.arch.lifecycle.LifecycleOwner;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.databinding.DeviceRowBinding;
import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.ui.base.BaseBindingAdapter;
import com.weebly.opus1269.clipman.viewmodel.DeviceViewModel;

/** Bridge between the Devices RecyclerView and the Devices class */
class DevicesAdapter extends BaseBindingAdapter<DeviceEntity, DeviceRowBinding, DevicesHandlers, DevicesAdapter.DeviceViewHolder> {

  DevicesAdapter(LifecycleOwner owner, DevicesHandlers handlers) {
    super(null, R.layout.device_row, owner, handlers);
  }

  @Override
  public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    DeviceRowBinding binding = DataBindingUtil.inflate(
      LayoutInflater.from(parent.getContext()),
      R.layout.device_row, parent, false);

    return new DeviceViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(DeviceViewHolder holder, int position) {
    final DeviceViewModel vm =
      new DeviceViewModel(App.INST(), getItem(position));
    holder.bind(vm, mHandlers);
  }

  /** ViewHolder inner class used to display the info. in the RecyclerView. */
  static class DeviceViewHolder extends RecyclerView.ViewHolder {
    private final DeviceRowBinding binding;

    DeviceViewHolder(DeviceRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    /** Bind the Device */
    void bind(DeviceViewModel vm, DevicesHandlers handlers) {
      binding.setVm(vm);
      binding.setHandlers(handlers);
      binding.executePendingBindings();
    }
  }
}
