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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.AdapterItem;
import com.weebly.opus1269.clipman.model.Clip;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Label;
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
public class ClipEntity implements Clip, AdapterItem, Serializable {
  public static final String TEXT_PLAIN = "text/plain";
  private static final String TAG = "ClipEntity";
  private static final String DESC_LABEL = "opus1269 was here";
  private static final String REMOTE_DESC_LABEL = "From Remote Copy";
  private static final String LABELS_LABEL = "ClipItem Labels";

  @PrimaryKey(autoGenerate = true)
  private long id;

  private String text;
  private long date;
  private boolean fav;
  private boolean remote;
  private String device;

  @Ignore
  private List<Label> labels = new ArrayList<>(0);

  /** PK's of the labels - only used for backup/restore */
  @Ignore
  private List<Long> labelsId = new ArrayList<>(0);

  public ClipEntity() {
    text = "";
    date = Instant.now().toEpochMilli();
    fav = false;
    remote = false;
    device = MyDevice.INST(App.INST()).getDisplayName();
  }

  @Ignore
  public ClipEntity(String text, long date, boolean fav, boolean remote,
                    String device) {
    this.text = text;
    this.date = date;
    this.fav = fav;
    this.remote = remote;
    this.device = device;
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
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String getText() {return text;}

  public void setText(String text) {
    this.text = text;
  }

  @Override
  public long getDate() {
    return date;
  }

  public void setDate(long date) {
    this.date = date;
  }

  @Override
  public boolean getFav() {
    return fav;
  }

  public void setFav(Boolean fav) {
    this.fav = fav;
  }

  @Override
  public boolean getRemote() {
    return remote;
  }

  public void setRemote(Boolean remote) {
    this.remote = remote;
    if (!remote) {
      setDevice(MyDevice.INST(App.INST()).getDisplayName());
    }
  }

  @Override
  public String getDevice() {
    return device;
  }

  public void setDevice(String device) {
    this.device = device;
  }

  public List<Label> getLabels() {
    return labels;
  }

  private void setLabels(List<Label> labels) {
    this.labels = labels;
  }

  public List<Long> getLabelsId() {
    return labelsId;
  }

  /**
   * Send to our devices
   * @param cntxt A Context
   */
  @Override
  public void send(Context cntxt) {
    if (User.INST(cntxt).isLoggedIn() && Prefs.INST(cntxt).isPushClipboard()) {
      MessagingClient.INST(cntxt).send(this);
    }
  }

  /** Copy to the clipboard */
  @Override
  public void copyToClipboard(@NonNull Context context) {
    final Handler handler = new Handler(Looper.getMainLooper());
    handler.post(() -> {
      final ClipboardManager clipboard =
        (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      if (clipboard != null) {
        final ClipData clip = ClipData.newPlainText(buildClipLabel(), text);
        clipboard.setPrimaryClip(clip);
        Log.logD(TAG, "copied to clipboard");
      }
    });
  }

  @Override
  public int hashCode() {
    return text.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClipEntity clipEntity = (ClipEntity) o;

    return text.equals(clipEntity.text);
  }

  @Override
  public String toString() {
    return "ClipEntity{" +
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

  public void setText(@NonNull Context context, @NonNull String text) {
    if (!text.equals(this.text)) {
      final String oldText = this.text;
      this.text = text;

      if (!TextUtils.isEmpty(oldText)) {
        // broadcast change to listeners
        final Intent intent = new Intent(Intents.FILTER_CLIP);
        final Bundle bundle = new Bundle();
        bundle.putString(Intents.ACTION_TYPE_CLIP,
          Intents.TYPE_TEXT_CHANGED_CLIP);
        bundle.putSerializable(Intents.EXTRA_CLIP, this);
        bundle.putString(Intents.EXTRA_TEXT, oldText);
        intent.putExtra(Intents.BUNDLE_CLIP, bundle);
        LocalBroadcastManager
          .getInstance(context)
          .sendBroadcast(intent);
      }
    }
  }

  /**
   * Do we have the given label
   * @param label a label
   * @return true if we have label
   */
  public boolean hasLabel(Label label) {
    return this.labels.contains(label);
  }

  //public void addLabel(Context context, Label label) {
  //  if (!hasLabel(label)) {
  //    this.labels.add(label);
  //    LabelTables.INST(context).insert(this, label);
  //  }
  //}
  //
  //public void removeLabel(Context context, Label label) {
  //  if (hasLabel(label)) {
  //    this.labels.remove(label);
  //    LabelTables.INST(context).delete(this, label);
  //  }
  //}
  //
  ///**
  // * Get our database PK
  // * @return table row, -1L if not found
  // */
  //public long getId(Context context) {
  //  return ClipTable.INST(context).getId(this);
  //}
  //
  ///**
  // * Get as a {@link ContentValues object}
  // * @return value
  // */
  //public ContentValues getContentValues() {
  //  final long fav = this.fav ? 1L : 0L;
  //  final long remote = this.remote ? 1L : 0L;
  //  final ContentValues cv = new ContentValues();
  //  cv.put(ClipsContract.Clip.COL_TEXT, text);
  //  cv.put(ClipsContract.Clip.COL_DATE, date);
  //  cv.put(ClipsContract.Clip.COL_FAV, fav);
  //  cv.put(ClipsContract.Clip.COL_REMOTE, remote);
  //  cv.put(ClipsContract.Clip.COL_DEVICE, device);
  //
  //  return cv;
  //}

  /**
   * Update the label id with the id of the given label - don't save
   * @param theLabel label with new id
   */
  public void updateLabelIdNoSave(@NonNull Label theLabel) {
    long newId = theLabel.getId();

    int pos = this.labels.indexOf(theLabel);
    if (pos != -1) {
      final long oldId = this.labels.get(pos).getId();
      this.labels.set(pos, theLabel);
      final int idPos = this.labelsId.indexOf(oldId);
      if (idPos != -1) {
        this.labelsId.set(idPos, newId);
      }
    }
  }

  /**
   * Add the given labels if they don't exit - don't save
   * @param labels label list to add from
   */
  public void addLabelsNoSave(@NonNull List<Label> labels) {
    for (Label label : labels) {
      if (!hasLabel(label)) {
        this.labels.add(label);
        this.labelsId.add(label.getId());
      }
    }
  }

  /**
   * Create a label with our state so we can restore it
   * @return a parsable label with our state
   */
  private CharSequence buildClipLabel() {
    final long fav = this.fav ? 1L : 0L;

    // add prefix and fav value
    CharSequence label = DESC_LABEL + "[" + Long.toString(fav) + "]\n";

    if (remote) {
      // add label indicating this is from a remote device
      label = label + REMOTE_DESC_LABEL + "(" + device + ")\n";
    }

    if (!AppUtils.isEmpty(this.labels)) {
      // add our labels
      final Gson gson = new Gson();
      final String labelsString = gson.toJson(this.labels);
      label = label + LABELS_LABEL + labelsString + "\n";
    }

    return label;
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

  ///**
  // * Delete from database
  // */
  //public void delete(Context context) {
  //  //return ClipTable.INST(context).delete(this);
  //}

  ///** Get our {@link Label} names from the database */
  //public void loadLabels(Context context) {
  //  this.labels.clear();
  //  this.labelsId.clear();
  //
  //  final List<Label> labels = LabelTables.INST(context).getLabels(this);
  //
  //  this.labels = labels;
  //  for (Label label : labels) {
  //    this.labelsId.add(label.getId());
  //  }
  //}
  //
}

