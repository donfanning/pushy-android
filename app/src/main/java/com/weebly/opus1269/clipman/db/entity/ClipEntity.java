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
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.model.AdapterItem;
import com.weebly.opus1269.clipman.model.Clip;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.MyDevice;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;

import org.threeten.bp.Instant;

import java.io.Serializable;
import java.lang.reflect.Type;
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
  private static final String ERROR_CLIPBOARD_READ = "Failed to read clipboard";

  @PrimaryKey(autoGenerate = true)
  private long id;
  
  private String text;
  private long date;
  private boolean fav;
  private boolean remote;
  private String device;

  @Ignore
  private List<Label> labels;

  /** PK's of the labels - only used for backup/restore */
  @Ignore
  private List<Long> labelsId;
  
  public ClipEntity() {}

  public ClipEntity(String text, long date, boolean fav, boolean remote, String device) {
    this.text = text;
    this.date = date;
    this.fav = fav;
    this.remote = remote;
    this.device = device;
  }

  public ClipEntity(Context context) {
    init(context);
  }

  public ClipEntity(Context context, String text) {
    init(context);
    this.text = text;
    //loadLabels(context);
  }

  public ClipEntity(Context context, String text, Instant instant,
                    Boolean fav,
                    @SuppressWarnings("SameParameterValue") Boolean remote,
                    String device) {
    init(context);
    this.text = text;
    this.date = instant.toEpochMilli();
    this.fav = fav;
    this.remote = remote;
    this.device = device;
    //loadLabels(context);
  }

  public ClipEntity(Context context, ClipEntity clipEntity) {
    init(context);
    this.text = clipEntity.getText();
    this.date = clipEntity.getDate();
    this.fav = clipEntity.getFav();
    this.remote = clipEntity.getRemote();
    this.device = clipEntity.getDevice();
    //loadLabels(context);
  }

  public ClipEntity(Context context, ClipEntity clipEntity,
                    List<Label> labels, List<Long> labelsId) {
    init(context);
    this.text = clipEntity.getText();
    this.date = clipEntity.getDate();
    this.fav = clipEntity.getFav();
    this.remote = clipEntity.getRemote();
    this.device = clipEntity.getDevice();
    this.labels = new ArrayList<>(labels);
    this.labelsId = new ArrayList<>(labelsId);
  }

  /**
   * Get the text on the Clipboard as a Clip
   * @param clipboard The manager
   * @return A new clip from the clipboard contents
   */
  @Nullable
  public static ClipEntity getFromClipboard(Context context,
                                            @Nullable
                                            ClipboardManager clipboard) {
    if (clipboard == null) {
      return null;
    }
    final ClipData clipData = clipboard.getPrimaryClip();
    if (clipData == null) {
      return null;
    }

    final ClipData.Item item = clipData.getItemAt(0);

    CharSequence clipText = item.getText();
    if (clipText == null) {
      // If the Uri contains something, just coerce it to text
      if (item.getUri() != null) {
        try {
          clipText = item.coerceToText(context);
        } catch (Exception ex) {
          Log.logEx(context, TAG, ex.getLocalizedMessage(), ex,
            ERROR_CLIPBOARD_READ);
          return null;
        }
      }
    }

    // parse the description for special instructions
    Boolean remote = false;
    String sourceDevice = MyDevice.INST(context).getDisplayName();
    final ClipDescription desc = clipData.getDescription();

    // set fav state if the copy is from us
    final Boolean fav = parseFav(desc);

    // set remote state if this is a remote copy
    final String parse = parseRemote(desc);
    if (!parse.isEmpty()) {
      remote = true;
      sourceDevice = parse;
    }

    // get any Labels
    final List<Label> labels = parseLabels(desc);

    ClipEntity clipEntity = null;
    if ((clipText != null) && (TextUtils.getTrimmedLength(clipText) > 0)) {
      clipEntity = new ClipEntity(context);
      clipEntity.setText(context, String.valueOf(clipText));
      clipEntity.setFav(fav);
      clipEntity.setRemote(remote);
      clipEntity.setDevice(sourceDevice);
      clipEntity.setLabels(labels);
    }

    return clipEntity;
  }

  /**
   * Send the clipboard contents to our devices
   * @param view toast parent
   */
  public static void sendClipboardContents(@NonNull Context context,
                                           @Nullable View view) {
    ClipboardManager clipboardManager =
      (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    final ClipEntity clipEntity = getFromClipboard(context, clipboardManager);
    int id = R.string.clipboard_no_text;

    if (!ClipEntity.isWhitespace(clipEntity)) {
      // TODO save to database
      //clipEntity.saveIfNew(context);

      // send to registered devices , if possible
      if (!User.INST(context).isLoggedIn()) {
        id = R.string.err_not_signed_in;
      } else if (!Prefs.INST(context).isDeviceRegistered()) {
        id = R.string.err_not_registered;
      } else if (!Prefs.INST(context).isPushClipboard()) {
        id = R.string.err_no_push;
      } else if (clipEntity.send(context)) {
        id = R.string.clipboard_sent;
      }
    }

    // display status message
    final String msg = context.getString(id);
    AppUtils.showMessage(context, view, msg);
  }

  /**
   * Is a {@link Clip} all whitespace
   * @param clip item
   * @return true if null of whitespace
   */
  public static boolean isWhitespace(Clip clip) {
    return (clip == null) || AppUtils.isWhitespace(clip.getText());
  }

  /**
   * Determine if a {@link ClipEntity} exists with given text and is a favorite
   * @param clipText text to query
   * @return true if exists and fav is true
   */
  public static boolean hasClipWithFav(Context context, String clipText) {
    return ClipTable.INST(context).exists(clipText, true);
  }

  /**
   * Get the label for the {@link ClipDescription}
   * @param desc The item's {@link ClipDescription}
   * @return true if null of whitespace
   */
  private static String getClipDescriptionLabel(ClipDescription desc) {
    String ret = "";

    if (desc != null) {
      final CharSequence label = desc.getLabel();
      if (label != null) {
        ret = label.toString();
      }
    }

    return ret;
  }

  /**
   * Parse the fav state from the {@link ClipDescription}
   * @param desc The item's {@link ClipDescription}
   * @return The fav state
   */
  private static boolean parseFav(ClipDescription desc) {
    boolean fav = false;

    String label = ClipEntity.getClipDescriptionLabel(desc);
    if (!TextUtils.isEmpty(label) && label.contains(DESC_LABEL)) {
      final int index = label.indexOf('[');
      if (index != -1) {
        label = label.substring(index + 1, index + 2);
        fav = Integer.parseInt(label) != 0;
      }
    }
    return fav;
  }

  /**
   * Parse the {@link ClipDescription} to see if it is from one of our
   * remote devices
   * @param desc The item's {@link ClipDescription}
   * @return The remote device name or "" if a local copy
   */
  private static String parseRemote(ClipDescription desc) {
    String device = "";

    final String label = ClipEntity.getClipDescriptionLabel(desc);
    if (!TextUtils.isEmpty(label) && label.contains(REMOTE_DESC_LABEL)) {
      final int idxStart = label.indexOf('(');
      final int idxStop = label.indexOf(')');
      device = label.substring(idxStart + 1, idxStop);
    }
    return device;
  }

  /**
   * Parse the {@link ClipDescription} to get any labels
   * @param desc The item's {@link ClipDescription}
   * @return The List of labels
   */
  private static List<Label> parseLabels(ClipDescription desc) {
    ArrayList<Label> list = new ArrayList<>(0);

    final String label = ClipEntity.getClipDescriptionLabel(desc);
    if (!TextUtils.isEmpty(label) && label.contains(LABELS_LABEL)) {
      final int idxStart = label.indexOf('\n' + LABELS_LABEL) +
        LABELS_LABEL.length();
      final int idxStop = label.indexOf('\n', idxStart);
      final String labelString = label.substring(idxStart + 1, idxStop);
      final Gson gson = new Gson();
      final Type type = new TypeToken<ArrayList<Label>>() {
      }.getType();
      list = gson.fromJson(labelString, type);
    }
    return list;
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
  }

  @Override
  public String getDevice() {
    return device;
  }

  public void setDevice(String device) {
    this.device = device;
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

  public void setText(@NonNull Context context, @NonNull String text) {
    if (!text.equals(this.text)) {
      final String oldText = this.text;
      this.text = text;

      if (!TextUtils.isEmpty(oldText)) {
        // broadcast change to listeners
        final Intent intent = new Intent(Intents.FILTER_CLIP_ITEM);
        final Bundle bundle = new Bundle();
        bundle.putString(Intents.ACTION_TYPE_CLIP_ITEM,
          Intents.TYPE_TEXT_CHANGED_CLIP_ITEM);
        bundle.putSerializable(Intents.EXTRA_CLIP_ITEM, this);
        bundle.putString(Intents.EXTRA_TEXT, oldText);
        intent.putExtra(Intents.BUNDLE_CLIP_ITEM, bundle);
        LocalBroadcastManager
          .getInstance(context)
          .sendBroadcast(intent);
      }
    }
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
   * Do we have the given label
   * @param label a label
   * @return true if we have label
   */
  public boolean hasLabel(Label label) {
    return this.labels.contains(label);
  }

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

  /** Copy to the clipboard */
  public void copyToClipboard(final Context context) {
    final Handler handler = new Handler(Looper.getMainLooper());
    handler.post(() -> {
      final ClipboardManager clipboard =
        (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      if (clipboard != null) {
        final ClipData clip = ClipData.newPlainText(buildClipLabel(), text);
        clipboard.setPrimaryClip(clip);
      }
    });
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
      if (view != null) {
        Snackbar
          .make(view, R.string.no_share, Snackbar.LENGTH_SHORT)
          .show();
      }
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
  // * Save to database
  // * @param onNewOnly if true, only save if text is not in database
  // * @return true if saved
  // */
  //@NonNull
  //private Boolean save(Context context, Boolean onNewOnly) {
  //  return ClipTable.INST(context).save(this, onNewOnly);
  //}
  //
  ///**
  // * Save to database if it is a new item
  // * @return true if saved
  // */
  //public Boolean saveIfNew(Context context) {
  //  return save(context, true);
  //}
  //
  ///**
  // * Save to database
  // * @return true if saved
  // */
  //public Boolean save(Context context) {
  //  return save(context, false);
  //}
  //
  ///**
  // * Delete from database
  // * @return true if deleted
  // */
  //public boolean delete(Context context) {
  //  return ClipTable.INST(context).delete(this);
  //}
  //
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
  /**
   * Send to our devices
   * @return true if sent
   */
  public Boolean send(Context context) {
    Boolean ret = false;
    if (User.INST(context).isLoggedIn() &&
      Prefs.INST(context).isPushClipboard()) {
      ret = true;
      // TODO
      //MessagingClient.INST(context).send(this);
    }
    return ret;
  }
  //
  /** Initialize the members */
  private void init(Context context) {
    text = "";
    date = Instant.now().toEpochMilli();
    fav = false;
    remote = false;
    device = MyDevice.INST(context).getDisplayName();
    labels = new ArrayList<>(0);
    labelsId = new ArrayList<>(0);
  }
}

