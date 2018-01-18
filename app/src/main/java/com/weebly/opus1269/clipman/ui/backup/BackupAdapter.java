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

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.databinding.BackupRowBinding;
import com.weebly.opus1269.clipman.model.BackupFile;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;
import com.weebly.opus1269.clipman.viewmodel.BackupViewModel;

import java.util.List;

/** Bridge between the BackupFile RecyclerView and the Backups class */
class BackupAdapter extends
  RecyclerView.Adapter<BackupAdapter.BackupViewHolder> {
  /** Our list */
  private List<BackupFile> mFiles;

  /** Our event handlers */
  private final BackupHandlers mHandlers;

  BackupAdapter(BackupHandlers handlers) {
    super();

    mHandlers = handlers;
  }

  @Override
  public BackupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    BackupRowBinding binding = DataBindingUtil
      .inflate(LayoutInflater.from(parent.getContext()), R.layout.backup_row,
        parent, false);

    return new BackupViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(BackupViewHolder holder, int position) {
    final BackupViewModel vm =
      new BackupViewModel(App.INST(), mFiles.get(position));
    holder.bind(vm, mHandlers);
  }

  @Override
  public int getItemCount() {return (mFiles == null) ? 0 : mFiles.size();}

  public void setList(@Nullable List<BackupFile> list) {
    // small list, just update it all
    mFiles = list;
    notifyDataSetChanged();
  }

  /** ViewHolder inner class used to display the info. in the RecyclerView. */
  static class BackupViewHolder extends RecyclerView.ViewHolder {
    private final BackupRowBinding binding;

    BackupViewHolder(@NonNull BackupRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      tintIcons();
    }

    /** Bind the File */
    void bind(BackupViewModel vm, BackupHandlers handlers) {
      binding.setVm(vm);
      binding.setHandlers(handlers);
      binding.executePendingBindings();
    }

    /** Color the Vector Drawables based on theme */
    private void tintIcons() {
      final Context context = this.itemView.getContext();
      final int color;

      if (Prefs.INST(context).isLightTheme()) {
        color = R.color.deep_teal_500;
      } else {
        color = R.color.deep_teal_200;
      }

      DrawableHelper
        .withContext(context)
        .withColor(color)
        .withDrawable(R.drawable.ic_cloud_download)
        .tint()
        .applyTo(binding.restoreButton);

      DrawableHelper
        .withContext(context)
        .withColor(color)
        .withDrawable(R.drawable.ic_cloud_sync)
        .tint()
        .applyTo(binding.syncButton);

      DrawableHelper
        .withContext(context)
        .withColor(color)
        .withDrawable(R.drawable.ic_delete_black_24dp)
        .tint()
        .applyTo(binding.deleteButton);
    }
  }
}
