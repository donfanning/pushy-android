/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.base;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ViewModel;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.AsyncListDiffer;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.AdapterItem;

import java.util.List;

/**
 * Abstract bridge between a {@link RecyclerView} and its' data
 * using a ViewModel and data binding
 */
public abstract class BaseBindingAdapter<T extends AdapterItem,
  U extends ViewDataBinding, V extends BaseHandlers, VM extends ViewModel,
  VH extends BaseViewHolder> extends RecyclerView.Adapter<VH> {
  /** Class identifier */
  private final String TAG = this.getClass().getSimpleName();

  /** Our LifecycleOwner */
  protected final LifecycleOwner mLifecycleOwner;

  /** Our layout */
  private final int mlayoutId;

  /** Our event handlers */
  private final V mHandlers;

  /** Helper to handle List changes */
  private final AsyncListDiffer<T> mDiffer;

  /** Factory to create a typed ViewHolder */
  private final VHAdapterFactory<VH, U> mVHFactory;

  /** Factory to create a typed ViewModel */
  private final VMAdapterFactory<VM, T> mVMFactory;

  protected BaseBindingAdapter(VHAdapterFactory<VH, U> holderFactory,
                               VMAdapterFactory<VM, T> modelFactory,
                               int layoutId, LifecycleOwner owner, V handlers) {
    mVHFactory = holderFactory;
    mVMFactory = modelFactory;
    mLifecycleOwner = owner;
    mHandlers = handlers;
    mlayoutId = layoutId;

    // create the AsyncListDiffer
    final DiffUtil.ItemCallback<T> diffCallback = new BaseDiffCallback();
    mDiffer = new AsyncListDiffer<>(this, diffCallback);

    setHasStableIds(true);
  }

  @Override
  @NonNull
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    U binding = DataBindingUtil.inflate(inflater, mlayoutId, parent, false);

    return mVHFactory.create(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    final VM vm = mVMFactory.create(getItem(position));
    //noinspection unchecked
    holder.bind(mLifecycleOwner, vm, mHandlers);
  }

  @Override
  public int getItemCount() {
    return mDiffer.getCurrentList().size();
  }

  @Override
  public long getItemId(int position) {
    return mDiffer.getCurrentList().get(position).getId();
  }

  /**
   * Pass a new List to the AdapterHelper.
   * Adapter updates will be computed on a background thread.
   * <p>
   * If a List is already present, a diff will be computed asynchronously
   * on a background thread.
   * When the diff is computed, it will be applied,
   * and the new List will be swapped in.
   *
   * @param list The new List.
   */
  public void submitList(List<T> list) {
    Log.logD(TAG, "submitting list");
    mDiffer.submitList(list);
  }

  private T getItem(int position) {
    return mDiffer.getCurrentList().get(position);
  }

  /** Class to determine what has changed in our list */
  class BaseDiffCallback extends DiffUtil.ItemCallback<T> {

    @Override
    public boolean areItemsTheSame(@NonNull T oldItem,
                                   @NonNull T newItem) {
      return oldItem.getId() == newItem.getId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull T oldItem,
                                      @NonNull T newItem) {
      final boolean equal = oldItem.equals(newItem);
      if (BuildConfig.DEBUG && !equal) {
        Log.logD(TAG, "contents changed:\n" + oldItem.toString() +
          '\n' + newItem.toString());
      }
      return equal;
    }
  }
}
