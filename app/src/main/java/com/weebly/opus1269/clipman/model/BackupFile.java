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

package com.weebly.opus1269.clipman.model;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.weebly.opus1269.clipman.app.Log;

import java.util.Map;
import java.util.UUID;

/** Immutable Class for a backup file's metadata */
public class BackupFile implements AdapterItem {
  private static long counter = 1L;

  private long id;
  private final Boolean isMine;
  private final DriveId driveId;
  private final String name;
  private String nickname;
  private String model;
  private String SN;
  private String OS;
  private final long date;

  public BackupFile(Context context, final Metadata metadata) {
    driveId = metadata.getDriveId();
    name = metadata.getTitle();
    date = metadata.getModifiedDate().getTime();
    model = "";
    SN = "";
    OS = "";
    nickname = "";

    final Map<CustomPropertyKey, String> props =
      metadata.getCustomProperties();
    for (Map.Entry<CustomPropertyKey, String> entry : props.entrySet()) {
      final String key = entry.getKey().getKey();
      final String value = entry.getValue();
      switch (key) {
        case "model":
          model = value;
          break;
        case "nickname":
          nickname = value;
          break;
        case "os":
          OS = value;
          break;
        case "sn":
          SN = value;
          break;
        default:
          break;
      }
    }

    isMine = isMyFile(context);

    id = counter;
    Log.logD("BackupFile", "id: " + id);
    counter++;
  }

  /**
   * Add the Drive CustomProperties for our device
   * @param context A Context
   * @param builder add CustomProperties to this
   */
  public static void setCustomProperties(Context context,
                                  MetadataChangeSet.Builder builder) {
    final int visibility = CustomPropertyKey.PRIVATE;
    builder
      .setCustomProperty(new CustomPropertyKey("model", visibility),
        MyDevice.INST(context).getModel())
      .setCustomProperty(new CustomPropertyKey("sn", visibility),
        MyDevice.INST(context).getSN())
      .setCustomProperty(new CustomPropertyKey("os", visibility),
        MyDevice.INST(context).getOS())
      .setCustomProperty(new CustomPropertyKey("nickname", visibility),
        MyDevice.INST(context).getNickname());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BackupFile that = (BackupFile) o;

    if (id != that.id) return false;
    if (date != that.date) return false;
    if (!isMine.equals(that.isMine)) return false;
    if (!driveId.equals(that.driveId)) return false;
    if (!name.equals(that.name)) return false;
    if (!nickname.equals(that.nickname)) return false;
    if (!model.equals(that.model)) return false;
    if (!SN.equals(that.SN)) return false;
    return OS.equals(that.OS);
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + isMine.hashCode();
    result = 31 * result + driveId.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + nickname.hashCode();
    result = 31 * result + model.hashCode();
    result = 31 * result + SN.hashCode();
    result = 31 * result + OS.hashCode();
    result = 31 * result + (int) (date ^ (date >>> 32));
    return result;
  }

  @Override
  public long getId() {
    return id;
  }

  public boolean isMine() {
    return isMine;
  }

  @NonNull
  public DriveId getDriveId() {
    return driveId;
  }

  @NonNull
  public String getName() {
    return name;
  }

  @NonNull
  public String getModel() {
    return model;
  }

  @NonNull
  public String getSN() {
    return SN;
  }

  @NonNull
  public String getOS() {
    return OS;
  }

  @NonNull
  public String getNickname() {
    return nickname;
  }

  public long getDate() {
    return date;
  }

  /**
   * Is this backup from this device
   * @param context A Context
   * @return true if from this device
   */
  private boolean isMyFile(@NonNull final Context context) {
    return (SN.equals(MyDevice.INST(context).getSN()) &&
      model.equals(MyDevice.INST(context).getModel()) &&
      OS.equals(MyDevice.INST(context).getOS()));
  }
}
