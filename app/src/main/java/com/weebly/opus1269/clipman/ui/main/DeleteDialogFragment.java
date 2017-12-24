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

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Analytics;

/** Modal dialog to delete all items in the main clip list */
public class DeleteDialogFragment extends DialogFragment {
  /** Screen name */
  private final String TAG = this.getClass().getSimpleName();

  /** Flag to indicate if the favorite items should be deleted as well */
  private Boolean mDeleteFavs = false;

  /** Activity must implement interface to get action events */
  private DeleteDialogListener mListener = null;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    Activity activity = getActivity();
    // Verify that the host activity implements the callback interface
    try {
      // Instantiate the DeleteDialogListener so we can send events to the host
      mListener = (DeleteDialogListener) activity;
    } catch (final ClassCastException ignored) {
      // The activity doesn't implement the interface, throw exception
      throw new ClassCastException(activity + " must implement " +
        "DeleteDialogListener");
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Context context = getContext();
    assert context != null;

    return new AlertDialog.Builder(context)
      .setTitle(R.string.delete_all_question)
      // only has one item
      .setMultiChoiceItems(R.array.favs, null,
        new DialogInterface.OnMultiChoiceClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which,
                              boolean checked) {
            mDeleteFavs = checked;
            Analytics.INST(context)
              .checkBoxClick(TAG, "includeFavs: " + checked);
          }
        })
      .setPositiveButton(R.string.button_delete, new DialogInterface
        .OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          Analytics.INST(context)
            .buttonClick(TAG, ((AlertDialog) dialog).getButton(which));
          // tell the listener to delete the items
          mListener.onDeleteDialogPositiveClick(mDeleteFavs);
        }
      })
      .setNegativeButton(R.string.button_cancel, new DialogInterface
        .OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          // User cancelled the dialog
          Analytics.INST(context)
            .buttonClick(TAG, ((AlertDialog) dialog).getButton(which));
          dialog.cancel();
        }
      })
      .create();
  }

  /**
   * The activity that creates an instance of this dialog fragment must
   * implement this interface in order to receive event callbacks.
   */
  public interface DeleteDialogListener {
    void onDeleteDialogPositiveClick(Boolean deleteFavs);
  }
}
