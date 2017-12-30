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

package com.weebly.opus1269.clipman.backup;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import org.joda.time.DateTime;

/** Immutable Class for a Drive file's metadata */
public class BackupFile {

  private final Boolean mIsMine;
  private final String mId;
  private final String mName;
  private final String mNickname;
  private final String mModel;
  private final String mSN;
  private final String mOS;
  private final long mDate;

  public BackupFile(String model, String sn, String os, String nickname) {
    mIsMine = false;
    mId = "";
    mName = "";
    mModel = model;
    mSN = sn;
    mOS = os;
    mNickname = nickname;
    mDate = new DateTime().getMillis();
  }

  /**
   * Get the appProperties for our device
   * @return {Gson} key-value pairs
   */
  @NonNull
  static Gson getAppProperties() {
    Gson value = new Gson();
    return value;
  }


  public boolean IsMine() {
    return mIsMine;
  }

  @NonNull
  public String getId() {
    return mId;
  }

  @NonNull
  public String getName() {
    return mName;
  }

  @NonNull
  public String getModel() {
    return mModel;
  }

  @NonNull
  public String getSN() {
    return mSN;
  }

  @NonNull
  public String getOS() {
    return mOS;
  }

  @NonNull
  public String getNickname() {
    return mNickname;
  }

  public DateTime getDate() {
    return new DateTime(mDate);
  }

  /**
   * Is this file more recent than the given file
   * @param file - file to compare to
   * @return true if newer
   */
  public boolean isNewer(@NonNull BackupFile file) {
    return this.mDate > file.mDate;
  }
}
