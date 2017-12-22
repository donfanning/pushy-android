/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import com.weebly.opus1269.clipman.db.ClipsContract;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.msg.MessagingClient;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** This class represents the data for a single clipboard entry */
public class ClipItem implements Serializable {
  public static final String TEXT_PLAIN = "text/plain";
  private static final String TAG = "ClipItem";
  private static final String DESC_LABEL = "opus1269 was here";
  private static final String REMOTE_DESC_LABEL = "From Remote Copy";
  private static final String LABELS_LABEL = "ClipItem Labels";
  private static final String ERROR_CLIPBOARD_READ = "Failed to read clipboard";

  private String mText;
  private DateTime mDate;
  private Boolean mFav;
  private Boolean mRemote;
  private String mDevice;
  private List<Label> mLabels;

  public ClipItem(Context context) {
    init(context);
  }

  public ClipItem(Context context, String text) {
    init(context);
    mText = text;
    loadLabels(context);
  }

  public ClipItem(Context context, String text, ReadableInstant date,
                  Boolean fav,
                  @SuppressWarnings("SameParameterValue") Boolean remote,
                  String device) {
    init(context);
    mText = text;
    mDate = new DateTime(date.getMillis());
    mFav = fav;
    mRemote = remote;
    mDevice = device;
    loadLabels(context);
  }

  public ClipItem(Context context, Cursor cursor) {
    init(context);
    int idx = cursor.getColumnIndex(ClipsContract.Clip.COL_TEXT);
    mText = cursor.getString(idx);
    idx = cursor.getColumnIndex(ClipsContract.Clip.COL_DATE);
    mDate = new DateTime(cursor.getLong(idx));
    idx = cursor.getColumnIndex(ClipsContract.Clip.COL_FAV);
    final long fav = cursor.getLong(idx);
    mFav = fav != 0L;
    idx = cursor.getColumnIndex(ClipsContract.Clip.COL_REMOTE);
    final long remote = cursor.getLong(idx);
    mRemote = remote != 0L;
    idx = cursor.getColumnIndex(ClipsContract.Clip.COL_DEVICE);
    mDevice = cursor.getString(idx);
    loadLabels(context);
  }

  public ClipItem(Context context, ClipItem clipItem) {
    init(context);
    mText = clipItem.getText();
    mDate = new DateTime(clipItem.getDate().getMillis());
    mFav = clipItem.isFav();
    mRemote = clipItem.isRemote();
    mDevice = clipItem.getDevice();
    loadLabels(context);
  }

  /**
   * Get the text on the Clipboard as a ClipItem
   * @param clipboard The manager
   * @return A new clip from the clipboard contents
   */
  @Nullable
  public static ClipItem getFromClipboard(Context context,
                                          ClipboardManager clipboard) {
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
    String sourceDevice = Device.getMyName(context);
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

    ClipItem clipItem = null;
    if ((clipText != null) && (TextUtils.getTrimmedLength(clipText) > 0)) {
      clipItem = new ClipItem(context);
      clipItem.setText(context, String.valueOf(clipText));
      clipItem.setFav(fav);
      clipItem.setRemote(remote);
      clipItem.setDevice(sourceDevice);
      clipItem.setLabels(labels);
    }

    return clipItem;
  }

  /**
   * Send the clipboard contents to our {@link Devices}
   * @param view toast parent
   */
  public static void sendClipboardContents(@NonNull Context context,
                                           @Nullable View view) {
    ClipboardManager clipboardManager =
      (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    final ClipItem clipItem = getFromClipboard(context, clipboardManager);
    int id = R.string.clipboard_no_text;

    if (!ClipItem.isWhitespace(clipItem)) {
      // save to database
      clipItem.saveIfNew(context);

      // send to registered devices , if possible
      if (!User.INST(context).isLoggedIn()) {
        id = R.string.err_not_signed_in;
      } else if (!Prefs.INST(context).isDeviceRegistered()) {
        id = R.string.err_not_registered;
      } else if (!Prefs.INST(context).isPushClipboard()) {
        id = R.string.err_no_push;
      } else if (clipItem.send(context)) {
        id = R.string.clipboard_sent;
      }
    }

    // display status message
    final String msg = context.getString(id);
    AppUtils.showMessage(context, view, msg);
  }

  /**
   * Is a {@link ClipItem} all whitespace
   * @param clipItem item
   * @return true if null of whitespace
   */
  public static boolean isWhitespace(ClipItem clipItem) {
    return (clipItem == null) || AppUtils.isWhitespace(clipItem.getText());
  }

  /**
   * Determine if a {@link ClipItem} exists with given text and is a favorite
   * @param clipText text to query
   * @return true if exists and fav is true
   */
  public static boolean hasClipWithFav(Context context, String clipText) {
    boolean ret = false;
    final ContentResolver resolver = context.getContentResolver();

    final String[] projection = {ClipsContract.Clip._ID};
    final String selection = ClipsContract.Clip.COL_TEXT + " = ? AND " +
      ClipsContract.Clip.COL_FAV + " = 1 ";
    final String[] selectionArgs = {clipText};

    final Cursor cursor = resolver.query(ClipsContract.Clip.CONTENT_URI,
      projection, selection, selectionArgs, null);

    if ((cursor != null) && (cursor.getCount() > 0)) {
      ret = true;
      cursor.close();
    }
    return ret;
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

    String label = ClipItem.getClipDescriptionLabel(desc);
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

    final String label = ClipItem.getClipDescriptionLabel(desc);
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

    final String label = ClipItem.getClipDescriptionLabel(desc);
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

  public String getText() {return mText;}

  public void setText(@NonNull Context context, @NonNull String text) {
    if (!text.equals(mText)) {
      final String oldText = mText;
      mText = text;

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

  public DateTime getDate() {return new DateTime(mDate.getMillis());}

  public void setDate(long date) {mDate = new DateTime(date);}

  public long getTime() {return mDate.getMillis();}

  public boolean isFav() {return mFav;}

  public void setFav(Boolean fav) {mFav = fav;}

  public boolean isRemote() {return mRemote;}

  public void setRemote(Boolean remote) {mRemote = remote;}

  public String getDevice() {return mDevice;}

  public void setDevice(String device) {mDevice = device;}

  public List<Label> getLabels() {
    return mLabels;
  }

  private void setLabels(List<Label> labels) {
    mLabels = labels;
  }

  /**
   * Do we have the given label
   * @param label a label
   * @return true if we have label
   */
  public boolean hasLabel(Label label) {
    return mLabels.contains(label);
  }

  public void addLabel(Context context, Label label) {
    if (!hasLabel(label)) {
      mLabels.add(label);
      LabelTables.INST(context).insert(this, label);
    }
  }

  public void removeLabel(Context context, Label label) {
    if (hasLabel(label)) {
      mLabels.remove(label);
      LabelTables.INST(context).delete(this, label);
    }
  }

  /**
   * Get our database PK
   * @return table row, -1L if not found
   */
  public long getId(Context context) {
    long ret = -1L;
    final ContentResolver resolver = context.getContentResolver();

    final String[] projection = {ClipsContract.Clip._ID};
    final String selection = ClipsContract.Clip.COL_TEXT + " = ? ";
    final String[] selectionArgs = {getText()};

    final Cursor cursor = resolver.query(ClipsContract.Clip.CONTENT_URI,
      projection, selection, selectionArgs, null);

    if ((cursor != null) && (cursor.getCount() > 0)) {
      cursor.moveToNext();
      ret = cursor.getLong(cursor.getColumnIndex(ClipsContract.Clip._ID));
      cursor.close();
      return ret;
    }
    return ret;
  }

  /**
   * Get as a {@link ContentValues object}
   * @return value
   */
  public ContentValues getContentValues() {
    final long fav = mFav ? 1L : 0L;
    final long remote = mRemote ? 1L : 0L;
    final ContentValues cv = new ContentValues();
    cv.put(ClipsContract.Clip.COL_TEXT, mText);
    cv.put(ClipsContract.Clip.COL_DATE, mDate.getMillis());
    cv.put(ClipsContract.Clip.COL_FAV, fav);
    cv.put(ClipsContract.Clip.COL_REMOTE, remote);
    cv.put(ClipsContract.Clip.COL_DEVICE, mDevice);

    return cv;
  }

  /** Copy to the clipboard */
  public void copyToClipboard(final Context context) {
    final Handler handler = new Handler(Looper.getMainLooper());
    handler.post(new Runnable() {

      @Override
      public void run() {
        final ClipboardManager clipboard = (ClipboardManager) context
          .getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText(buildClipLabel(), mText);
        if (clipboard != null) {
          clipboard.setPrimaryClip(clip);
        }
      }
    });
  }

  /**
   * Create a label with our state so we can restore it
   * @return a parsable label with our state
   */
  private CharSequence buildClipLabel() {
    final long fav = mFav ? 1L : 0L;

    // add prefix and fav value
    CharSequence label = DESC_LABEL + "[" + Long.toString(fav) + "]\n";

    if (mRemote) {
      // add label indicating this is from a remote device
      label = label + REMOTE_DESC_LABEL + "(" + mDevice + ")\n";
    }

    if (mLabels.size() > 0) {
      // add our labels
      final Gson gson = new Gson();
      final String labelsString = gson.toJson(mLabels);
      label = label + LABELS_LABEL + labelsString + "\n";
    }

    return label;
  }

  /**
   * Share the ClipItem with other apps
   * @param ctxt A context
   * @param view The {@link View} that is requesting the share
   */
  public void doShare(Context ctxt, @Nullable View view) {
    if (TextUtils.isEmpty(mText)) {
      if (view != null) {
        Snackbar
          .make(view, R.string.no_share, Snackbar.LENGTH_SHORT)
          .show();
      }
      return;
    }

    final long fav = mFav ? 1L : 0L;
    // add a label with the fav value so our watcher can maintain the state
    CharSequence label = DESC_LABEL;
    label = label + Long.toString(fav);

    final Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_TEXT, mText);
    intent.putExtra(Intent.EXTRA_TITLE, label);
    intent.setType(TEXT_PLAIN);
    final Intent sendIntent = Intent.createChooser(intent,
      ctxt.getResources().getString(R.string.share_text_to));
    AppUtils.startNewTaskActivity(ctxt, sendIntent);
  }

  /**
   * Save to database
   * @param onNewOnly if true, only save if text is not in database
   * @return true if saved
   */
  @NonNull
  private Boolean save(Context context, Boolean onNewOnly) {
    if (ClipItem.isWhitespace(this)) {
      return false;
    }
    final long id = getId(context);
    final boolean exists = (id != -1L);

    if (onNewOnly && exists) {
      // already exists
      return false;
    }

    final ContentResolver resolver = context.getContentResolver();

    if (exists) {
      // update
      final Uri uri =
        ContentUris.withAppendedId(ClipsContract.Clip.CONTENT_URI, id);
      resolver.update(uri, getContentValues(), null, null);
    } else {
      // insert new
      resolver.insert(ClipsContract.Clip.CONTENT_URI, getContentValues());

      // add the label map entries
      List<Label> labels = getLabels();
      for (Label label : labels) {
        LabelTables.INST(context).insert(this, label);
      }
    }

    return true;
  }

  /**
   * Save to database if it is a new item
   * @return true if saved
   */
  public Boolean saveIfNew(Context context) {
    return save(context, true);
  }

  /**
   * Save to database
   * @return true if saved
   */
  public Boolean save(Context context) {
    return save(context, false);
  }

  /**
   * Delete from database
   * @return true if deleted
   */
  public Boolean delete(Context context) {
    if (ClipItem.isWhitespace(this)) {
      return false;
    }

    final ContentResolver resolver = context.getContentResolver();

    final String selection = ClipsContract.Clip.COL_TEXT + " = ? ";
    final String[] selectionArgs = {getText()};

    // do it
    final int nRows =
      resolver.delete(ClipsContract.Clip.CONTENT_URI, selection, selectionArgs);

    return (nRows == 1);
  }

  /** Get our {@link Label} names from the database */
  public void loadLabels(Context context) {
    mLabels.clear();

    if (ClipItem.isWhitespace(this)) {
      return;
    }

    final ContentResolver resolver = context.getContentResolver();

    final String[] projection = {ClipsContract.LabelMap.COL_LABEL_NAME};
    final long id = getId(context);
    final String selection = ClipsContract.LabelMap.COL_CLIP_ID + " = " + id;

    Cursor cursor = resolver.query(ClipsContract.LabelMap.CONTENT_URI,
      projection, selection, null, null);
    if (cursor == null) {
      return;
    }

    try {
      while (cursor.moveToNext()) {
        final int idx =
          cursor.getColumnIndex(ClipsContract.LabelMap.COL_LABEL_NAME);
        final String name = cursor.getString(idx);
        mLabels.add(new Label(name));
      }
    } finally {
      cursor.close();
    }
  }

  /**
   * Send to our devices
   * @return true if sent
   */
  public Boolean send(Context context) {
    Boolean ret = false;
    if (User.INST(context).isLoggedIn() &&
      Prefs.INST(context).isPushClipboard()) {
      ret = true;
      MessagingClient.INST(context).send(this);
    }
    return ret;
  }

  /** Initialize the members */
  private void init(Context context) {
    mText = "";
    mDate = new DateTime();
    mFav = false;
    mRemote = false;
    mDevice = Device.getMyName(context);
    mLabels = new ArrayList<>(0);
  }
}

