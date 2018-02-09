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

package com.weebly.opus1269.clipman.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.weebly.opus1269.clipman.model.AdapterItem;
import com.weebly.opus1269.clipman.model.MyDevice;

import java.util.Map;

/** A Goole Drive backup of our data */
@Entity(tableName = "backups", indices = {@Index(value = "drive_id_invariant", unique = true)})
public class Backup implements AdapterItem {
  @PrimaryKey(autoGenerate = true)
  private long id;

  @ColumnInfo(name = "drive_id_string")
  private String driveIdString;

  @ColumnInfo(name = "drive_id_invariant")
  private String driveIdInvariant;

  private Boolean isMine;
  private String name;
  private String nickname;
  private String model;
  private String SN;
  private String OS;
  private long date;

  public Backup() {}

  public Backup(Context context, final Metadata metadata) {
    driveIdString = metadata.getDriveId().encodeToString();
    driveIdInvariant = metadata.getDriveId().toInvariantString();
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
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Boolean getMine() {
    return isMine;
  }

  public void setMine(Boolean mine) {
    isMine = mine;
  }

  public DriveId getDriveId() {
    return DriveId.decodeFromString(driveIdString);
  }

  public String getDriveIdString() {
    return driveIdString;
  }

  public void setDriveIdString(String driveIdString) {
    this.driveIdString = driveIdString;
  }

  public String getDriveIdInvariant() {
    return driveIdInvariant;
  }

  public void setDriveIdInvariant(String driveIdInvariant) {
    this.driveIdInvariant = driveIdInvariant;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getSN() {
    return SN;
  }

  public void setSN(String SN) {
    this.SN = SN;
  }

  public String getOS() {
    return OS;
  }

  public void setOS(String OS) {
    this.OS = OS;
  }

  public long getDate() {
    return date;
  }

  public void setDate(long date) {
    this.date = date;
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Backup that = (Backup) o;

    if (date != that.date) return false;
    if (!driveIdString.equals(that.driveIdString)) return false;
    if (!driveIdInvariant.equals(that.driveIdInvariant)) return false;
    if (!name.equals(that.name)) return false;
    if (!nickname.equals(that.nickname)) return false;
    if (!model.equals(that.model)) return false;
    if (!SN.equals(that.SN)) return false;
    return OS.equals(that.OS);
  }

  @Override
  public int hashCode() {
    int result = driveIdString.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + nickname.hashCode();
    result = 31 * result + model.hashCode();
    result = 31 * result + SN.hashCode();
    result = 31 * result + OS.hashCode();
    result = 31 * result + (int) (date ^ (date >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "Backup{" +
      "id=" + id +
      ", isMine=" + isMine +
      ", driveIdString='" + driveIdString + '\'' +
      ", name='" + name + '\'' +
      ", nickname='" + nickname + '\'' +
      ", model='" + model + '\'' +
      ", SN='" + SN + '\'' +
      ", OS='" + OS + '\'' +
      ", date=" + date +
      '}';
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
