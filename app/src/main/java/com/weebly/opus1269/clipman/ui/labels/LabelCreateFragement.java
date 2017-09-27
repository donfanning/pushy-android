/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/**
 * Fragment to Create a new {@link Label}.
 * to handle interaction events.
 */
public class LabelCreateFragement extends Fragment implements
  View.OnClickListener,
  View.OnKeyListener,
  TextWatcher {

  public LabelCreateFragement() {
    // Required empty public constructor
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View rootView =
      inflater.inflate(R.layout.fragment_label_create, container, false);

    setup(rootView);

    return rootView;
  }

  @Override
  public void onClick(View view) {
    if (view.getId() == R.id.addDoneButton) {
      final EditText editText = getActivity().findViewById(R.id.addText);
      addLabel(editText);
    }
  }

  @Override
  public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

  }

  @Override
  public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

  }

  @Override
  public void afterTextChanged(Editable editable) {
    final ImageButton doneButton =
      getActivity().findViewById(R.id.addDoneButton);
    final String text = editable.toString();
    final boolean enabled = (TextUtils.getTrimmedLength(text) > 0);
    setImageViewEnabled(doneButton, enabled);
  }

  @Override
  public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
    if (view.getId() == R.id.addText) {
      if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
        (keyCode == KeyEvent.KEYCODE_ENTER)) {
        addLabel((EditText) view);
        // dismiss keyboard
        InputMethodManager imm = (InputMethodManager)
          getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        return true;
      }
      return false;
    }

    return false;
  }

  private void setup(View rootView) {
    int color;

    final ImageView addImage = rootView.findViewById(R.id.addImage);
    final EditText addText = rootView.findViewById(R.id.addText);
    final ImageButton addDoneButton = rootView.findViewById(R.id.addDoneButton);

    // listen for clicks
    addDoneButton.setOnClickListener(this);

    // listen for text changes and key presses
    addText.addTextChangedListener(this);
    addText.setOnKeyListener(this);

    setImageViewEnabled(addDoneButton, false);

    // tint icons
    if (Prefs.isLightTheme()) {
      color = R.color.deep_teal_500;
    } else {
      color = R.color.deep_teal_200;
    }
    DrawableHelper
      .withContext(getContext())
      .withColor(color)
      .withDrawable(R.drawable.ic_add)
      .tint()
      .applyTo(addImage);

    if (Prefs.isLightTheme()) {
      color = android.R.color.primary_text_light;
    } else {
      color = android.R.color.primary_text_dark;
    }
    DrawableHelper
      .withContext(getContext())
      .withColor(color)
      .withDrawable(R.drawable.ic_done)
      .tint()
      .applyTo(addDoneButton);
  }

  private void setImageViewEnabled(ImageView button, boolean enabled) {
    final int alpha = enabled ? 255 : 64;
    button.setEnabled(enabled);
    button.setImageAlpha(alpha);
  }

  private boolean addLabel(EditText editText) {
    boolean ret = false;
    String text = editText.getText().toString();
    if (!TextUtils.isEmpty(text)) {
      text = text.trim();
      if (text.length() > 0) {
        final Label label = new Label(text);
        ret = label.save();
      }
    }
    return ret;
  }
}
