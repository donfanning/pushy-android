/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Prefs;

/** Modal dialog to display list of sort types for the clip list */
public class SortTypeDialogFragment extends DialogFragment {
  /** Screen name */
  private final String TAG = this.getClass().getSimpleName();

  /** Activity must implement interface to get action events */
  private SortTypeDialogFragment.SortTypeDialogListener mListener = null;

  /** The actual dialog */
  private AlertDialog mDialog;

  // Override the Fragment.onAttach() method to instantiate the listener
  @SuppressWarnings("ObjectToString")
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    final Activity activity = getActivity();
    // Verify that the host activity implements the callback interface
    try {
      // Instantiate the DeleteDialogListener so we can send events to the host
      mListener = (SortTypeDialogFragment.SortTypeDialogListener) activity;
    } catch (final ClassCastException ignored) {
      // The activity doesn't implement the interface, throw exception
      throw new ClassCastException(
        activity + " must implement SortTypeDialogListener");
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Context context = getContext();
    assert context != null;

    // current setting
    final int selected = Prefs.INST(getContext()).getSortType();

    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder
      .setSingleChoiceItems(R.array.sort_type_clips, selected,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Analytics.INST(context).checkBoxClick(TAG, "sortType: " + which);
            Prefs.INST(context).setSortType(which);
            // tell the listener something was elected
            mListener.onSortTypeSelected();
            dialog.dismiss();
          }
        });

    // Create the AlertDialog
    mDialog = builder.create();
    mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

    return mDialog;
  }

  @Override
  public void onStart() {
    super.onStart();

    final Window window = mDialog.getWindow();
    if (window != null) {

      // position and size the dialog at the top right of the window
      WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
      lp.copyFrom(window.getAttributes());
      lp.gravity = Gravity.TOP | Gravity.END;
      lp.x = 0;
      lp.y = 0;
      lp.width = AppUtils.dp2px(getContext(), 200.0F);
      window.setAttributes(lp);
    }
  }

  /**
   * The activity that creates an instance of this dialog fragment must
   * implement this interface in order to receive event callbacks.
   */
  public interface SortTypeDialogListener {
    void onSortTypeSelected();
  }
}
