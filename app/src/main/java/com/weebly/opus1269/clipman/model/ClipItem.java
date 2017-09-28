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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.ClipsContract;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.msg.MessagingClient;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** This class represents the data for a single clipboard entry */
public class ClipItem implements Serializable {
  public static final String TEXT_PLAIN = "text/plain";
  private static final String TAG = "ClipItem";
  private static final String DESC_LABEL = "opus1269 was here";
  private static final String REMOTE_DESC_LABEL = "From Remote Copy";
  private static final String ERROR_CLIPBOARD_READ = "Failed to read clipboard";

  private String mText;
  private DateTime mDate;
  private Boolean mFav;
  private Boolean mRemote;
  private String mDevice;
  private List<Label> mLabels;

  public ClipItem() {
    init();
  }

  public ClipItem(String text) {
    init();
    mText = text;
    loadLabels();
  }

  public ClipItem(String text, ReadableInstant date, Boolean fav,
                  Boolean remote, String device) {
    mText = text;
    mDate = new DateTime(date.getMillis());
    mFav = fav;
    mRemote = remote;
    mDevice = device;
    loadLabels();
  }

  public ClipItem(Cursor cursor) {
    init();
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
    loadLabels();
  }

  public ClipItem(ClipItem clipItem) {
    init();
    mText = clipItem.getText();
    mDate = new DateTime(clipItem.getDate().getMillis());
    mFav = clipItem.isFav();
    mRemote = clipItem.isRemote();
    mDevice = clipItem.getDevice();
    loadLabels();
  }

  /**
   * Get the text on the Clipboard as a ClipItem
   * @param clipboard The manager
   * @return A new clip from the clipboard contents
   */
  @Nullable
  public static ClipItem getFromClipboard(ClipboardManager clipboard) {
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
          clipText = item.coerceToText(App.getContext());
        } catch (Exception ex) {
          Log.logEx(TAG, ex.getLocalizedMessage(), ex, ERROR_CLIPBOARD_READ);
          return null;
        }
      }
    }

    // parse the description for special instructions
    Boolean remote = false;
    String sourceDevice = Device.getMyName();
    final ClipDescription desc = clipData.getDescription();
    // set fav state if the copy is from us
    final Boolean fav = parseFav(desc);
    // set remote state if this is a remote copy
    final String parse = parseRemote(desc);
    if (!parse.isEmpty()) {
      remote = true;
      sourceDevice = parse;
    }

    ClipItem clipItem = null;
    if ((clipText != null) && (TextUtils.getTrimmedLength(clipText) > 0)) {
      clipItem = new ClipItem(String.valueOf(clipText));
      clipItem.setFav(fav);
      clipItem.setRemote(remote);
      clipItem.setDevice(sourceDevice);
    }

    return clipItem;
  }

  /**
   * Send the clipboard contents to our {@link Devices}
   */
  public static void sendClipboardContents(View view) {
    final Context context = App.getContext();
    ClipboardManager clipboardManager =
      (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    final ClipItem clipItem = getFromClipboard(clipboardManager);
    int id = R.string.clipboard_no_text;

    if ((clipItem != null) && !TextUtils.isEmpty(clipItem.getText())) {

      // save to DB
      clipItem.saveIfNew();

      // send to registered devices , if possible
      if (!User.INST.isLoggedIn()) {
        id = R.string.err_not_signed_in;
      } else if (!Prefs.isDeviceRegistered()) {
        id = R.string.err_not_registered;
      } else if (!Prefs.isPushClipboard()) {
        id = R.string.err_no_push;
      } else if (clipItem.send()) {
        id = R.string.clipboard_sent;
      }
    }

    // display status message
    final String msg = context.getString(id);
    AppUtils.showMessage(view, msg);
  }

  /**
   * Parse the fav state from the {@link ClipDescription}
   * @param desc The item's {@link ClipDescription}
   * @return The fav state
   */
  private static boolean parseFav(ClipDescription desc) {
    boolean fav = false;

    String label = (String) desc.getLabel();
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

    final String label = (String) desc.getLabel();
    if (!TextUtils.isEmpty(label) && label.contains(REMOTE_DESC_LABEL)) {
      final int idxStart = label.indexOf('(');
      final int idxStop = label.indexOf(')');
      device = label.substring(idxStart + 1, idxStop);
    }
    return device;
  }

  public String getText() {
    return mText;
  }

  public void setText(String text) {
    mText = text;
  }

  public DateTime getDate() {
    return new DateTime(mDate.getMillis());
  }

  @SuppressWarnings("unused")
  public void setDate(ReadableInstant date) {
    mDate = new DateTime(date.getMillis());
  }

  public void setDate(long date) {
    mDate = new DateTime(date);
  }

  public long getTime() {
    return mDate.getMillis();
  }

  public boolean isFav() {
    return mFav;
  }

  public void setFav(Boolean fav) {
    mFav = fav;
  }

  public boolean isRemote() {
    return mRemote;
  }

  public void setRemote(Boolean remote) {
    mRemote = remote;
  }

  public String getDevice() {
    return mDevice;
  }

  public void setDevice(String device) {
    mDevice = device;
  }

  /**
   * Do we have the given label
   * @param label a label
   * @return true if we have label
   */
  public boolean hasLabel(Label label) {
    for (Label label_ : mLabels) {
      if (label_.getName().equals(label.getName())) {
        return true;
      }
    }
    return false;
  }

  public void addLabel(Label label) {
    mLabels.add(label);
    LabelTables.INST.insert(this, label);
  }

  public void removeLabel(Label label) {
    mLabels.remove(label);
    LabelTables.INST.delete(this, label);
  }

  /**
   * Get the ClipItem as a {@link ContentValues object}
   * @return ClipItem as {@link ContentValues object}
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

  /**
   * Copy the ClipItem to the clipboard
   */
  public void copyToClipboard() {
    // Make sure we have looper
    final Handler handler = new Handler(Looper.getMainLooper());
    handler.post(new Runnable() {

      @Override
      public void run() {
        final ClipboardManager clipboard = (ClipboardManager) App
          .getContext()
          .getSystemService(Context.CLIPBOARD_SERVICE);
        final long fav = mFav ? 1L : 0L;

        // add a label with the fav value so we can maintain the state
        CharSequence label =
          DESC_LABEL + "[" + Long.toString(fav) + "]\n";
        if (mRemote) {
          // add label indicating this is from a remote device
          label = label + REMOTE_DESC_LABEL + "(" + mDevice + ")";
        }

        final ClipData clip = ClipData.newPlainText(label, mText);
        clipboard.setPrimaryClip(clip);
      }
    });
  }

  /**
   * Share the ClipItem with other apps
   * @param view The {@link View} that is requesting the share
   */
  public void doShare(View view) {
    if (TextUtils.isEmpty(mText)) {
      if (view != null) {
        Snackbar
          .make(view, R.string.no_share, Snackbar.LENGTH_SHORT)
          .show();
      }
      return;
    }

    Context context;
    if (view != null) {
      context = view.getContext();
    } else {
      context = App.getContext();
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
      context.getResources().getString(R.string.share_text_to));
    AppUtils.startNewTaskActivity(sendIntent);
  }

  /**
   * Save to database
   * @param onNewOnly if true, only save if text is not in database
   * @return true if saved
   */
  @NonNull
  private Boolean save(Boolean onNewOnly) {
    return ClipTable.INST.insert(this, onNewOnly);
  }

  /**
   * Save to database if it is a new item
   * @return true if saved
   */
  public Boolean saveIfNew() {
    return save(true);
  }

  /**
   * Save to database
   * @return true if saved
   */
  public Boolean save() {
    return save(false);
  }

  /**
   * Send to our devices
   * @return true if sent
   */
  public Boolean send() {
    Boolean ret = false;
    if (User.INST.isLoggedIn() && Prefs.isPushClipboard()) {
      ret = true;
      MessagingClient.send(this);
    }
    return ret;
  }

  /**
   * Get our {@link Label} names from the database
   */
  private void loadLabels() {
    mLabels.clear();
    final Cursor cursor = LabelTables.INST.getLabelNames(this);
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
   * Initialize the members
   */
  private void init() {
    mText = "";
    mDate = new DateTime();
    mFav = false;
    mRemote = false;
    mDevice = Device.getMyName();
    mLabels = new ArrayList<>(0);
  }
}
