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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/** Bridge between the Backup RecyclerView and the Backups class */
class BackupAdapter extends
  RecyclerView.Adapter<BackupAdapter.BackupViewHolder> {

  /** Our activity */
  private final BaseActivity mActivity;

  BackupAdapter(BaseActivity activity) {
    super();

    mActivity = activity;
  }

  @Override
  public BackupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final Context context = parent.getContext();
    final LayoutInflater inflater = LayoutInflater.from(context);

    // Inflate the custom layout
    final View view = inflater.inflate(R.layout.backup_row, parent, false);

    // Return a new holder instance
    return new BackupViewHolder(view);
  }

  @Override
  public void onBindViewHolder(BackupViewHolder holder, int position) {
    final Context context = holder.backupTextView.getContext();

    tintIcons(holder);

    // Get the data model based on position
    // TODO set File
    final Device device = Devices.INST(mActivity).get(position);

    final String desc =
      context.getString(R.string.backup_nickname_fmt, device.getNickname()) +
        '\n' +
        context.getString(R.string.backup_model_fmt, device.getModel()) + '\n' +
        context.getString(R.string.backup_SN_fmt, device.getSN()) + '\n' +
        context.getString(R.string.backup_OS_fmt, device.getOS());
    final TextView backupTextView = holder.backupTextView;
    backupTextView.setText(desc);

    final CharSequence value =
      AppUtils.getRelativeDisplayTime(context, device.getLastSeen());
    final TextView dateTextView = holder.dateTextView;
    dateTextView.setText(context.getString(R.string.backup_date_fmt,
      value));

    holder.deleteButton.setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Devices.INST(mActivity).remove(device);
          Analytics.INST(v.getContext())
            .imageClick("BackupActivity", "deleteBackup");
        }
      }
    );

    holder.restoreButton.setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Devices.INST(mActivity).remove(device);
          Analytics.INST(v.getContext())
            .imageClick("BackupActivity", "restoreBackup");
        }
      }
    );
  }

  @Override
  public int getItemCount() {return Devices.INST(mActivity).getCount();}

  /**
   * Color the Vector Drawables based on theme
   * @param holder BackupViewHolder
   */
  private void tintIcons(BackupAdapter.BackupViewHolder holder) {
    final Context context = holder.deleteButton.getContext();
    final int color;

    if (Prefs.INST(context).isLightTheme()) {
      color = R.color.deep_teal_500;
    } else {
      color = R.color.deep_teal_200;
    }

    DrawableHelper
      .withContext(context)
      .withColor(color)
      .withDrawable(R.drawable.ic_delete_black_24dp)
      .tint()
      .applyTo(holder.deleteButton);

    DrawableHelper
      .withContext(context)
      .withColor(color)
      .withDrawable(R.drawable.ic_cloud_download)
      .tint()
      .applyTo(holder.restoreButton);
  }

  /** ViewHolder inner class used to display the info. in the RecyclerView. */
  static class BackupViewHolder extends RecyclerView.ViewHolder {
    final TextView dateTextView;
    final TextView backupTextView;
    final ImageButton restoreButton;
    final ImageButton deleteButton;


    BackupViewHolder(View view) {
      super(view);

      dateTextView = view.findViewById(R.id.backupDate);
      backupTextView = view.findViewById(R.id.backupText);
      restoreButton = view.findViewById(R.id.restoreButton);
      deleteButton = view.findViewById(R.id.deleteButton);
    }
  }
}
