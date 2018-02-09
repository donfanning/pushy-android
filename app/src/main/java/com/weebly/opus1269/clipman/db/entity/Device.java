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
import android.text.TextUtils;

import com.weebly.opus1269.clipman.model.AdapterItem;

import org.threeten.bp.Instant;

/** A (hopefully) unique hardware device */
@Entity(tableName = "devices",
  indices = {@Index(value = {"model", "SN", "OS"}, unique = true)})
public class Device implements AdapterItem {
  @PrimaryKey(autoGenerate = true)
  private long id;

  // These three should be unique (not for emulators)
  private String model;
  private String SN;
  private String OS;

  private String nickname;
  @ColumnInfo(name = "last_seen")
  private long lastSeen;

  public Device() {
  }

  public Device(String model, String sn, String os, String nickname) {
    this.model = model;
    this.SN = sn;
    this.OS = os;
    this.nickname = nickname;
    this.lastSeen = Instant.now().toEpochMilli();
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Device that = (Device) o;

    if (id != that.id) return false;
    if (lastSeen != that.lastSeen) return false;
    if (!model.equals(that.model)) return false;
    if (!SN.equals(that.SN)) return false;
    if (!OS.equals(that.OS)) return false;
    return nickname.equals(that.nickname);
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + model.hashCode();
    result = 31 * result + SN.hashCode();
    result = 31 * result + OS.hashCode();
    result = 31 * result + nickname.hashCode();
    result = 31 * result + (int) (lastSeen ^ (lastSeen >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "Device{" +
      "id=" + id +
      ", model='" + model + '\'' +
      ", SN='" + SN + '\'' +
      ", OS='" + OS + '\'' +
      ", nickname='" + nickname + '\'' +
      ", lastSeen=" + lastSeen +
      '}';
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public void setSN(String SN) {
    this.SN = SN;
  }

  public void setOS(String OS) {
    this.OS = OS;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getModel() {
    return model;
  }

  public String getSN() {
    return SN;
  }

  public String getOS() {
    return OS;
  }

  public String getNickname() {
    return nickname;
  }

  public long getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(long lastSeen) {
    this.lastSeen = lastSeen;
  }

  /** A String suitable for display */
  public String getDisplayName() {
    String name = getNickname();
    if (TextUtils.isEmpty(name)) {
      name = getModel() + " - " + getSN() + " - " + getOS();
    }
    return name;
  }

  /** A String that is unique for a Device */
  public String getUniqueName() {
    return getModel() + " - " + getSN() + " - " + getOS();
  }
}
