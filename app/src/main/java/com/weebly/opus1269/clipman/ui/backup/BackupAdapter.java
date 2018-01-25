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
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.databinding.BackupRowBinding;
import com.weebly.opus1269.clipman.db.entity.BackupEntity;
import com.weebly.opus1269.clipman.ui.base.BaseBindingAdapter;
import com.weebly.opus1269.clipman.ui.base.BaseViewHolder;
import com.weebly.opus1269.clipman.ui.base.VHAdapterFactory;
import com.weebly.opus1269.clipman.viewmodel.BackupViewModel;
import com.weebly.opus1269.clipman.ui.base.VMAdapterFactory;

/** Bridge between the BackupEntity RecyclerView and the Backups class */
class BackupAdapter extends BaseBindingAdapter<BackupEntity, BackupRowBinding, BackupHandlers, BackupViewModel, BackupAdapter.BackupViewHolder> {

  BackupAdapter(LifecycleOwner owner, BackupHandlers handlers) {
    super(new BackupViewHolderFactory(), new BackupViewModelFactory(),
      R.layout.backup_row, owner, handlers);
  }

  /** Factory to create an instance of our ViewHolder */
  static class BackupViewHolderFactory implements
    VHAdapterFactory<BackupViewHolder, BackupRowBinding> {
    BackupViewHolderFactory() {}

    @Override
    public BackupViewHolder create(BackupRowBinding binding) {
      return new BackupViewHolder(binding);
    }
  }
  
  /** Factory to create an instance of our ViewModel */
  static class BackupViewModelFactory implements
    VMAdapterFactory<BackupViewModel, BackupEntity> {

    @Override
    public BackupViewModel create(BackupEntity item) {
      return new BackupViewModel(App.INST(), item);
    }
  }
  
  /** Our ViewHolder */
  static class BackupViewHolder extends
    BaseViewHolder<BackupRowBinding, BackupViewModel, BackupHandlers> {

    BackupViewHolder(@NonNull BackupRowBinding binding) {
      super(binding);
    }

    /** Bind the File */
    public void bind(LifecycleOwner owner, BackupViewModel vm,
                     BackupHandlers handlers) {
      super.bind(owner, vm, handlers);
    }
  }
}
