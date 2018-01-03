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

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Label;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Immutable Class for a backup file's metadata */
public class BackupFile {

  private Contents mContents;
  private final Boolean mIsMine;
  private final DriveId mId;
  private final String mName;
  private String mNickname;
  private String mModel;
  private String mSN;
  private String mOS;
  private final long mDate;

  BackupFile(Context context, final Metadata metadata) {
    mContents = new Contents();
    mId = metadata.getDriveId();
    mName = metadata.getTitle();
    mDate = metadata.getModifiedDate().getTime();
    mModel = "";
    mSN = "";
    mOS = "";
    mNickname = "";

    final Map<CustomPropertyKey, String> props =
      metadata.getCustomProperties();
    for (Map.Entry<CustomPropertyKey, String> entry : props.entrySet()) {
      final String key = entry.getKey().getKey();
      final String value = entry.getValue();
      switch (key) {
        case "model":
          mModel = value;
          break;
        case "nickname":
          mNickname = value;
          break;
        case "os":
          mOS = value;
          break;
        case "sn":
          mSN = value;
          break;
        default:
          break;
      }
    }

    mIsMine = isMyFile(context);
  }

  /** Set the Drive CustomProperties for our device */
  static void setCustomProperties(Context context,
                                  MetadataChangeSet.Builder builder) {
    builder
      .setCustomProperty(
        new CustomPropertyKey("model", CustomPropertyKey.PRIVATE),
        Device.getMyModel())
      .setCustomProperty(
        new CustomPropertyKey("sn", CustomPropertyKey.PRIVATE),
        Device.getMySN(context))
      .setCustomProperty(
        new CustomPropertyKey("os", CustomPropertyKey.PRIVATE),
        Device.getMyOS())
      .setCustomProperty(
        new CustomPropertyKey("nickname", CustomPropertyKey.PRIVATE),
        Device.getMyNickname(context));
  }

  public Contents getContents() {
    return mContents;
  }

  public boolean isMine() {
    return mIsMine;
  }

  @NonNull
  public DriveId getId() {
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

  private boolean isMyFile(@NonNull final Context context) {
    return (mSN.equals(Device.getMySN(context)) &&
      mModel.equals(Device.getMyModel()) &&
      mOS.equals(Device.getMyOS()));
  }

  /** Inner class for the contents of a backup */
  static class Contents {
    private List<Label> labels;
    private List<ClipItem> clipItems;

    Contents() {
      this.labels = new ArrayList<>(0);
      this.clipItems = new ArrayList<>(0);
    }

    Contents(@NonNull List<Label> labels,
                    @NonNull List<ClipItem> clipItems) {
      this.labels = labels;
      this.clipItems = clipItems;
    }

    public List<Label> getLabels() {
      return labels;
    }

    public List<ClipItem> getClipItems() {
      return clipItems;
    }
  }
}
