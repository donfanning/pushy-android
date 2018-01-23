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

package com.weebly.opus1269.clipman.ui.backup;

import android.arch.lifecycle.LifecycleOwner;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.databinding.BackupRowBinding;
import com.weebly.opus1269.clipman.model.BackupFile;
import com.weebly.opus1269.clipman.ui.base.BaseBindingAdapter;
import com.weebly.opus1269.clipman.viewmodel.BackupViewModel;

import java.util.List;

/** Bridge between the BackupFile RecyclerView and the Backups class */
class BackupAdapter extends BaseBindingAdapter<BackupFile, BackupRowBinding, BackupHandlers, BackupAdapter.BackupViewHolder> {

  BackupAdapter(LifecycleOwner owner, BackupHandlers handlers) {
    super(null, R.layout.backup_row, owner, handlers);
  }

  @Override
  public void setList(List<BackupFile> list) {
    super.setList(list);
    // TODO diff stuff not working why?
    notifyDataSetChanged();
  }

  @Override
  public BackupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    BackupRowBinding binding = DataBindingUtil
      .inflate(LayoutInflater.from(parent.getContext()), mlayoutId,
        parent, false);

    return new BackupViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(BackupViewHolder holder, int position) {
    final BackupViewModel vm =
      new BackupViewModel(App.INST(), getItem(position));
    holder.bind(vm, mHandlers);
  }

  /** ViewHolder inner class used to display the info. in the RecyclerView. */
  static class BackupViewHolder extends RecyclerView.ViewHolder {
    private final BackupRowBinding binding;

    BackupViewHolder(@NonNull BackupRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    /** Bind the File */
    void bind(BackupViewModel vm, BackupHandlers handlers) {
      binding.setVm(vm);
      binding.setHandlers(handlers);
      binding.executePendingBindings();
    }
  }
}
