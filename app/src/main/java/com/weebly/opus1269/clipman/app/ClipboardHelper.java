/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.app;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.MyDevice;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.repos.MainRepo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Static methods for interacting with the system clipboard */
public class ClipboardHelper {
  private static final String TAG = "ClipboardHelper";

  private static final String DESC_LABEL = "opus1269 was here";
  private static final String REMOTE_DESC_LABEL = "From Remote Copy";
  private static final String LABELS_LABEL = "ClipItem Labels";
  private static final String ERROR_CLIPBOARD_READ = "Failed to read clipboard";

  /**
   * Get the text on the Clipboard as a Clip
   * @param clipboard The manager
   * @return A new clip from the clipboard contents
   */
  @Nullable
  public static ClipEntity getFromClipboard(Context context,
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
      clipEntity = new ClipEntity();
      clipEntity.setText(context, String.valueOf(clipText));
      clipEntity.setFav(fav);
      clipEntity.setRemote(remote);
      clipEntity.setDevice(sourceDevice);
      // TODO clipEntity.setLabels(labels);
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
      MainRepo.INST(App.INST()).addClipIfNewAsync(clipEntity, true);

      // send to registered devices , if possible
      if (!User.INST(context).isLoggedIn()) {
        id = R.string.err_not_signed_in;
      } else if (!Prefs.INST(context).isDeviceRegistered()) {
        id = R.string.err_not_registered;
      } else if (!Prefs.INST(context).isPushClipboard()) {
        id = R.string.err_no_push;
      } else {
        id = R.string.clipboard_sent;
      }

      clipEntity.send(context);
    }

    // display status message
    final String msg = context.getString(id);
    AppUtils.showMessage(context, view, msg);
  }

  /**
   * Get the label for the {@link ClipDescription}
   * @param desc The description
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

    String label = ClipboardHelper.getClipDescriptionLabel(desc);
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

    final String label = ClipboardHelper.getClipDescriptionLabel(desc);
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

    final String label = ClipboardHelper.getClipDescriptionLabel(desc);
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

}
