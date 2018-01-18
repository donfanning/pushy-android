/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.model.BackupFile;

/** ViewModel for a {@link BackupFile} */
public class BackupViewModel extends AndroidViewModel {

  /** Our File */
  public final BackupFile backupFile;

  public BackupViewModel(@NonNull Application app, BackupFile backupFile) {
    super(app);

    this.backupFile = backupFile;
  }
}
