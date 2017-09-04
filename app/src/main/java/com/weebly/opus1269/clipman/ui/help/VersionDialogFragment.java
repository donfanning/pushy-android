/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.help;

import android.annotation.SuppressLint;
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
import com.weebly.opus1269.clipman.model.Prefs;

public class VersionDialogFragment extends DialogFragment {

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    // Get the inflater and inflate the view
    // Pass null as the parent view because its going in the dialog layout
    LayoutInflater inflater = getActivity().getLayoutInflater();
    @SuppressLint("InflateParams")
    View view = inflater.inflate(R.layout.dialog_version, null);

    // set version name
    TextView version = view.findViewById(R.id.version);
    version.setText(getResources().getString(R.string.version_fmt, Prefs.getVersionName()));

    // linkify license
    // http://stackoverflow.com/a/16003280/4468645
    TextView license = view.findViewById(R.id.license);
    Linkify.addLinks(license, Linkify.WEB_URLS);
    license.setMovementMethod(ArrowKeyMovementMethod.getInstance());
    license.setTextIsSelectable(true);

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setView(view);

    return builder.create();
  }

}
