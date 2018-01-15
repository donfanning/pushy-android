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

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.databinding.DeviceRowBinding;
import com.weebly.opus1269.clipman.model.device.Device;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.viewmodel.DeviceViewModel;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

import java.util.List;

/** Bridge between the Devices RecyclerView and the Devices class */
class DevicesAdapter extends
  RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {

  /** Our event handlers */
  private final DevicesHandlers mHandlers;

  /** Our Activity */
  private DevicesActivity mActivity = null;

  /** Our List */
  private List<? extends Device> mDevices = null;

  DevicesAdapter(DevicesActivity activity,
                 DevicesHandlers handlers,
                 LiveData<List<Device>> deviceList) {
    super();

    mActivity = activity;
    mHandlers = handlers;
    mDevices = deviceList.getValue();

    // Observe devices
    deviceList.observe(activity, devices -> {
      if (devices != null) {
        mDevices = devices;
        notifyDataSetChanged();
      }
    });
  }

  @Override
  public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    DeviceRowBinding binding = DataBindingUtil.inflate(
      LayoutInflater.from(parent.getContext()),
      R.layout.device_row, parent, false);

    // Return a new holder instance
    return new DeviceViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(DeviceViewHolder holder, int position) {
    DeviceViewModel vm =
      new DeviceViewModel(mActivity.getApplication(), mDevices.get(position));
    holder.bind(vm, mHandlers);
  }

  @Override
  public int getItemCount() {return mDevices.size();}

  /** ViewHolder inner class used to display the info. in the RecyclerView. */
  static class DeviceViewHolder extends RecyclerView.ViewHolder {
    private final DeviceRowBinding binding;

    DeviceViewHolder(DeviceRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      tintIcons();
    }

    /** Bind the Device */
    void bind(DeviceViewModel vm, DevicesHandlers handlers) {
      binding.setVm(vm);
      binding.setHandlers(handlers);
      binding.executePendingBindings();
    }

    /** Color the Vector Drawables based on theme */
    private void tintIcons() {
      final ImageButton forgetButton = binding.forgetButton;
      final Context context = forgetButton.getContext();
      final int color;

      if (Prefs.INST(context).isLightTheme()) {
        color = R.color.deep_teal_500;
      } else {
        color = R.color.deep_teal_200;
      }

      DrawableHelper
        .withContext(context)
        .withColor(color)
        .withDrawable(R.drawable.ic_clear)
        .tint()
        .applyTo(forgetButton);
    }
  }
}
