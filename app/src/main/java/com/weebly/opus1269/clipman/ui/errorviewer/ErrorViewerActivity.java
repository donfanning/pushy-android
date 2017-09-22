/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.errorviewer;

import android.os.Bundle;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

public class ErrorViewerActivity extends BaseActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_error_viewer;

    super.onCreate(savedInstanceState);
  }

}
