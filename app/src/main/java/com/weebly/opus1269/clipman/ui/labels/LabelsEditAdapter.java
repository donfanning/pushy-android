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

package com.weebly.opus1269.clipman.ui.labels;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Labels;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/**
 * Bridge between the RecyclerView and the {@link Labels} class
 */
class LabelsEditAdapter extends RecyclerView.Adapter<LabelsEditAdapter.LabelViewHolder> {

  @Override
  public LabelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final Context context = parent.getContext();
    final LayoutInflater inflater = LayoutInflater.from(context);

    // Inflate the custom layout
    final View view = inflater.inflate(R.layout.label_edit_row, parent, false);

    // Return a new holder instance
    return new LabelViewHolder(view);
  }

  @Override
  public void onBindViewHolder(LabelViewHolder holder, int position) {
    tintIcons(holder);

    // Get the data model based on position
    final Label label = Labels.get(position);

    final EditText labelEditText = holder.labelEditText;
    labelEditText.setText(label.getName());
    holder.deleteButton.setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Labels.remove(label);
        }
      }
    );
  }

  @Override
  public int getItemCount() {
    return Labels.getCount();
  }

  /**
   * Color the Vector Drawables based on theme
   * @param holder LabelViewHolder
   */
  private void tintIcons(LabelViewHolder holder) {
    final Context context = holder.labelImage.getContext();
    int color;

    if (Prefs.isLightTheme()) {
      color = R.color.deep_teal_500;
    } else {
      color = R.color.deep_teal_200;
    }
    DrawableHelper
      .withContext(context)
      .withColor(color)
      .withDrawable(R.drawable.ic_label)
      .tint()
      .applyTo(holder.labelImage);

    if (Prefs.isLightTheme()) {
      color = android.R.color.primary_text_light;
    } else {
      color = android.R.color.primary_text_dark;
    }
    DrawableHelper
      .withContext(context)
      .withColor(color)
      .withDrawable(R.drawable.ic_clear_black_24dp)
      .tint()
      .applyTo(holder.deleteButton);


  }

  static class LabelViewHolder extends RecyclerView.ViewHolder {
    final ImageView labelImage;
    final EditText labelEditText;
    final ImageButton deleteButton;

    LabelViewHolder(View view) {
      super(view);

      labelImage = view.findViewById(R.id.labelImage);
      labelEditText = view.findViewById(R.id.labelText);
      deleteButton = view.findViewById(R.id.deleteButton);
    }
  }
}
