/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.viewmodel;

import android.app.Application;
import android.support.annotation.NonNull;

import com.weebly.opus1269.clipman.db.entity.Backup;
import com.weebly.opus1269.clipman.repos.BackupRepo;

/** ViewModel for a {@link Backup} */
public class BackupViewModel extends BaseRepoViewModel<BackupRepo> {
  /** Our backup */
  @NonNull
  private final Backup backup;

  public BackupViewModel(@NonNull Application app,
                         @NonNull Backup backup) {
    super(app, BackupRepo.INST(app));

    this.backup = backup;
  }

  @NonNull
  public Backup getBackup() {
    return backup;
  }
}
