/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.base;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ViewModel;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;

import com.weebly.opus1269.clipman.BR;

/** Base class for a ViewHolder with data binding */
public class BaseViewHolder<T extends ViewDataBinding, U extends ViewModel,
  V extends BaseHandlers> extends RecyclerView.ViewHolder {
  public final T binding;
  public U vm;
  public V handlers;

  public BaseViewHolder(T binding) {
    super(binding.getRoot());
    this.binding = binding;
  }

  public void bind(LifecycleOwner owner, U vm, V handlers) {
    this.vm = vm;
    this.handlers = handlers;
    binding.setLifecycleOwner(owner);
    binding.setVariable(BR.vm, vm);
    binding.setVariable(BR.handlers, handlers);
    binding.executePendingBindings();
  }
}
