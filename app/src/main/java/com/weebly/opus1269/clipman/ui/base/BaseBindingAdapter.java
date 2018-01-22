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
 * WIBaseEntityHOUBaseEntity WARRANBaseEntityIES OR CONDIBaseEntityIONS OF
 * ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.weebly.opus1269.clipman.ui.base;

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.DiffCallback;
import android.support.v7.recyclerview.extensions.ListAdapterConfig;
import android.support.v7.recyclerview.extensions.ListAdapterHelper;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;

import com.weebly.opus1269.clipman.db.entity.BaseEntity;

import java.util.List;

/** Abstract bridge between a {@link RecyclerView} and its' data */
public abstract class BaseBindingAdapter<T extends BaseEntity, VH extends
  RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
  /** Our LifecycleOwner */
  protected final LifecycleOwner mLifecycleOwner;
  /** Our layout */
  protected final int mlayoutId;
  /** Our event handlers */
  protected final BaseHandlers mHandlers;
  private final ListAdapterHelper<T> mHelper;
  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  public BaseBindingAdapter(int layoutId, LifecycleOwner owner,
                            BaseHandlers handlers) {
    final DiffCallback<T> diffCallback = new BaseDiffCallback<>();
    mHelper = new ListAdapterHelper<>(new AdapterCallback(this),
      new ListAdapterConfig.Builder<T>().setDiffCallback(diffCallback).build());
    mLifecycleOwner = owner;
    mHandlers = handlers;
    mlayoutId = layoutId;
  }

  /**
   * Set the new list to be displayed.
   * <p>
   * If a list is already being displayed, a diff will be computed on a
   * background thread, which
   * will dispatch Adapter.notifyItem events on the main thread.
   * @param list The new list to be displayed.
   */
  public void setList(List<T> list) {
    mHelper.setList(list);
  }

  @SuppressWarnings("unused")
  protected T getItem(int position) {
    return mHelper.getItem(position);
  }

  @Override
  public int getItemCount() {
    return mHelper.getItemCount();
  }

  /** Inner class to handle updates to our list */
  public static class AdapterCallback implements ListUpdateCallback {
    private final RecyclerView.Adapter mAdapter;

    AdapterCallback(RecyclerView.Adapter adapter) {
      mAdapter = adapter;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      mAdapter.notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position, count, payload);
    }
  }

  /** Inner class to determine what has changed in our list */
  class BaseDiffCallback<T extends BaseEntity> extends DiffCallback<T> {

    @Override
    public boolean areItemsTheSame(@NonNull T oldItem,
                                   @NonNull T newItem) {
      return oldItem.getId() == newItem.getId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull T oldItem,
                                      @NonNull T newItem) {
      return oldItem.equals(newItem);
    }
  }

  //static class BaseViewHolder extends RecyclerView.ViewHolder {
  //  private final LabelEditRowBinding binding;
  //
  //  BaseViewHolder(@NonNull LabelEditRowBinding binding) {
  //    super(binding.getRoot());
  //    this.binding = binding;
  //  }
  //
  //  /** Bind the data */
  //  void bind(LifecycleOwner owner, LabelViewModel vm, LabelHandlers
  // handlers) {
  //    binding.setLifecycleOwner(owner);
  //    binding.setVm(vm);
  //    binding.setHandlers(handlers);
  //    binding.executePendingBindings();
  //  }
  //}
}
