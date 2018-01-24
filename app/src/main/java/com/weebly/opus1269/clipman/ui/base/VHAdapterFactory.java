/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.base;

import android.databinding.ViewDataBinding;

/** Factory to create typed ViewHolder instances */
public interface VHAdapterFactory<VH, T extends ViewDataBinding> {
  VH create(T binding);
}
