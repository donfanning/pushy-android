/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.annotations.Expose;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.AdapterItem;
import com.weebly.opus1269.clipman.model.MyDevice;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;

import org.threeten.bp.Instant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** This class represents the data for a single clipboard entry */
@Entity(tableName = "clips", indices = {@Index(value = "text", unique = true)})
public class Clip implements AdapterItem, Serializable {
  public static final String TEXT_PLAIN = "text/plain";

  private static final String DESC_LABEL = "opus1269 was here";

  @PrimaryKey(autoGenerate = true)
  private long id;

  @Expose
  private String text;

  @Expose
  private long date;

  @Expose
  private boolean fav;

  @Expose
  private boolean remote;

  @Expose
  private String device;

  /** The Labels - TODO this should be done with Relation or something */
  @Expose
  @Ignore
  @NonNull
  private List<Label> labels = new ArrayList<>(0);

  /** PK's of the labels - only used for backup/restore */
  @Expose
  @Ignore
  @NonNull
  private List<Long> labelsId = new ArrayList<>(0);

  public Clip() {
    text = "";
    date = Instant.now().toEpochMilli();
    fav = false;
    remote = false;
    device = MyDevice.INST(App.INST()).getDisplayName();
  }

  @Ignore
  public Clip(String text, long date, boolean fav, boolean remote,
              String device) {
    this.text = text;
    this.date = date;
    this.fav = fav;
    this.remote = remote;
    this.device = device;
  }

  public Clip(Clip clip, List<Label> labels, List<Long> labelsId) {
    this.text = clip.getText();
    this.date = clip.getDate();
    this.fav = clip.getFav();
    this.remote = clip.getRemote();
    this.device = clip.getDevice();
    this.labels = new ArrayList<>(labels);
    this.labelsId = new ArrayList<>(labelsId);
  }

  /**
   * Is a {@link Clip} all whitespace
   * @param clip item
   * @return true if null of whitespace
   */
  public static boolean isWhitespace(@Nullable Clip clip) {
    return clip == null || AppUtils.isWhitespace(clip.getText());
  }

  @Override
  public int hashCode() {
    int result = text.hashCode();
    result = 31 * result + (int) (date ^ (date >>> 32));
    result = 31 * result + (fav ? 1 : 0);
    result = 31 * result + (remote ? 1 : 0);
    result = 31 * result + device.hashCode();
    return result;
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Clip that = (Clip) o;

    if (date != that.date) return false;
    if (fav != that.fav) return false;
    if (remote != that.remote) return false;
    if (!text.equals(that.text)) return false;
    return device.equals(that.device);
  }

  @Override
  public String toString() {
    return "Clip{" +
      "id=" + id +
      ", text='" + text + '\'' +
      ", date=" + date +
      ", fav=" + fav +
      ", remote=" + remote +
      ", device='" + device + '\'' +
      ", labels=" + labels +
      ", labelsId=" + labelsId +
      '}';
  }

  @Override
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public long getDate() {
    return date;
  }

  public void setDate(long date) {
    this.date = date;
  }

  public boolean getFav() {
    return fav;
  }

  public void setFav(Boolean fav) {
    this.fav = fav;
  }

  public boolean getRemote() {
    return remote;
  }

  public void setRemote(Boolean remote) {
    this.remote = remote;
    if (!remote) {
      setDevice(MyDevice.INST(App.INST()).getDisplayName());
    }
  }

  public String getDevice() {
    return device;
  }

  public void setDevice(String device) {
    this.device = device;
  }

  @NonNull
  public List<Label> getLabels() {
    return labels;
  }

  public void setLabels(@Nullable List<Label> labels) {
    this.labels = new ArrayList<>(0);
    labelsId = new ArrayList<>();
    if (!AppUtils.isEmpty(labels)) {
      this.labels.addAll(labels);
      for (final Label label : this.labels) {
        labelsId.add(label.getId());
      }
    }
  }

  @NonNull
  public List<Long> getLabelsId() {
    return labelsId;
  }

  /**
   * Send to our devices
   * @param cntxt A Context
   */
  public void send(Context cntxt) {
    if (User.INST(cntxt).isLoggedIn() && Prefs.INST(cntxt).isPushClipboard()) {
      MessagingClient.INST(cntxt).send(this);
    }
  }

  /**
   * Add a Label
   * @param label Label
   */
  public void addLabel(@NonNull Label label) {
    if (!hasLabel(label)) {
      this.labels.add(label);
    }
  }

  /**
   * Remove a Label
   * @param label Label
   */
  public void removeLabel(@NonNull Label label) {
    if (hasLabel(label)) {
      this.labels.remove(label);
    }
  }

  /**
   * Add labels if they don't exist
   * @param labels Labels
   */
  public void addLabels(@NonNull List<Label> labels) {
    for (Label label : labels) {
      if (!hasLabel(label)) {
        this.labels.add(label);
        this.labelsId.add(label.getId());
      }
    }
  }

  /**
   * Update the label id with the id of the given label
   * @param label label with new id
   */
  public void updateLabelId(@NonNull Label label) {
    long newId = label.getId();

    int pos = this.labels.indexOf(label);
    if (pos != -1) {
      final long oldId = this.labels.get(pos).getId();
      this.labels.set(pos, label);
      final int idPos = this.labelsId.indexOf(oldId);
      if (idPos != -1) {
        this.labelsId.set(idPos, newId);
      }
    }
  }

  /**
   * Share the Clip with other apps
   * @param ctxt A context
   * @param view The {@link View} that is requesting the share
   */
  public void doShare(Context ctxt, @Nullable View view) {
    if (TextUtils.isEmpty(text)) {
      AppUtils.showMessage(ctxt, view, ctxt.getString(R.string.no_share));
      return;
    }

    final long fav = this.fav ? 1L : 0L;
    // add a label with the fav value so our watcher can maintain the state
    CharSequence label = DESC_LABEL;
    label = label + Long.toString(fav);

    final Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_TEXT, text);
    intent.putExtra(Intent.EXTRA_TITLE, label);
    intent.setType(TEXT_PLAIN);
    final Intent sendIntent = Intent.createChooser(intent,
      ctxt.getResources().getString(R.string.share_text_to));
    AppUtils.startNewTaskActivity(ctxt, sendIntent);
  }

  /**
   * Do we have the given label
   * @param label a label
   * @return true if we have label
   */
  private boolean hasLabel(@NonNull Label label) {
    return this.labels.contains(label);
  }
}

