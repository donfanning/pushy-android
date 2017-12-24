/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.help;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.method.ArrowKeyMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Prefs;

/** Display version info. and copyright dialog */
public class VersionDialogFragment extends DialogFragment {
  /** Screen name */
  private final String TAG = this.getClass().getSimpleName();

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Activity activity = getActivity();
    assert activity != null;

    // Get the inflater and inflate the view
    // Pass null as the parent view because its going in the dialog layout
    final LayoutInflater inflater = activity.getLayoutInflater();
    @SuppressLint("InflateParams")
    final View view = inflater.inflate(R.layout.dialog_version, null);

    // set version name
    final TextView version = view.findViewById(R.id.version);
    final String vName = Prefs.INST(activity).getVersionName();
    version.setText(getString(R.string.version_fmt, vName));

    // linkify license
    // http://stackoverflow.com/a/16003280/4468645
    final TextView license = view.findViewById(R.id.license);
    try {
      Linkify.addLinks(license, Linkify.WEB_URLS);
    } catch (Exception ex) {
      Log.logEx(activity, TAG, ex.getLocalizedMessage(), ex, false);
    }
    license.setMovementMethod(ArrowKeyMovementMethod.getInstance());
    license.setTextIsSelectable(true);

    return new AlertDialog.Builder(activity).setView(view).create();
  }
}
