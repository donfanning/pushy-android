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

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.databinding.DeviceRowBinding;
import com.weebly.opus1269.clipman.db.entity.DeviceEntity;
import com.weebly.opus1269.clipman.ui.base.BaseBindingAdapter;
import com.weebly.opus1269.clipman.ui.base.BaseViewHolder;
import com.weebly.opus1269.clipman.ui.base.ViewHolderFactory;
import com.weebly.opus1269.clipman.viewmodel.DeviceViewModel;

/** Bridge between the Devices RecyclerView and the Devices class */
class DevicesAdapter extends BaseBindingAdapter<DeviceEntity, DeviceRowBinding,
  DevicesHandlers, DevicesAdapter.DeviceViewHolder> {

  DevicesAdapter(LifecycleOwner owner, DevicesHandlers handlers) {
    super(new DeviceViewHolderFactory(), R.layout.device_row, owner, handlers);
  }

  @Override
  public void onBindViewHolder(DeviceViewHolder holder, int position) {
    final DeviceViewModel vm =
      new DeviceViewModel(App.INST(), getItem(position));
    holder.bind(mLifecycleOwner, vm, mHandlers);
  }

  /** Factory to create an instance of our ViewHolder */
  static class DeviceViewHolderFactory implements
    ViewHolderFactory<DeviceViewHolder, DeviceRowBinding> {
    DeviceViewHolderFactory() {}

    @Override
    public DeviceViewHolder create(DeviceRowBinding binding) {
      return new DeviceViewHolder(binding);
    }
  }

  /** Our ViewHolder */
  static class DeviceViewHolder extends
    BaseViewHolder<DeviceRowBinding, DeviceViewModel, DevicesHandlers> {

    DeviceViewHolder(DeviceRowBinding binding) {
      super(binding);
    }

    /** Bind the Device */
    public void bind(LifecycleOwner owner, DeviceViewModel vm,
                     DevicesHandlers handlers) {
      super.bind(owner, vm, handlers);
    }
  }
}
