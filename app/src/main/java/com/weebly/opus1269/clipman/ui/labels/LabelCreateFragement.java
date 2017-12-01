/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/** Fragment to Create a new {@link Label} */
public class LabelCreateFragement extends Fragment implements
  TextView.OnEditorActionListener,
  View.OnClickListener,
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
  public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
    if (textView.getId() == R.id.addText) {
      if (id == EditorInfo.IME_ACTION_DONE) {
        doneAction();
        return true;
      } else if (keyEvent != null) {
        final int keyAction = keyEvent.getAction();
        if (keyAction == KeyEvent.ACTION_DOWN) {
          // eat it
          return true;
        } else if (keyAction == KeyEvent.ACTION_UP) {
          doneAction();
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void onClick(View view) {
    if (view.getId() == R.id.addDoneButton) {
      final BaseActivity activity = (BaseActivity)getActivity();
      doneAction();
      Analytics.INST(activity).imageClick(activity.getTAG(),
        "addLabel");
    }
  }

  @Override
  public void beforeTextChanged(CharSequence text, int i, int i1, int i2) {
    // noop
  }

  @Override
  public void onTextChanged(CharSequence text, int i, int i1, int i2) {
    //noop
  }

  @Override
  public void afterTextChanged(Editable editable) {
    final ImageButton doneButton =
      getActivity().findViewById(R.id.addDoneButton);
    final String text = editable.toString();
    final boolean enabled = (TextUtils.getTrimmedLength(text) > 0);
    DrawableHelper.setImageViewEnabled(doneButton, enabled);
  }

  /**
   * Initialize the view
   * @param rootView The View
   */
  private void setup(View rootView) {
    int color;

    final ImageView addImage = rootView.findViewById(R.id.addImage);
    final EditText addText = rootView.findViewById(R.id.addText);
    final ImageButton addDoneButton = rootView.findViewById(R.id.addDoneButton);

    // listen for clicks
    addDoneButton.setOnClickListener(this);

    // listen for text changes and actions
    addText.addTextChangedListener(this);
    addText.setOnEditorActionListener(this);

    DrawableHelper.setImageViewEnabled(addDoneButton, false);

    // tint icons
    if (Prefs.INST(getContext()).isLightTheme()) {
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

    if (Prefs.INST(getContext()).isLightTheme()) {
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

  /** Handle label creation */
  private boolean doneAction() {
    boolean ret = false;
    final EditText editText = getActivity().findViewById(R.id.addText);
    String text = editText.getText().toString();
    if (!AppUtils.isWhitespace(text)) {
      text = text.trim();
      ret = new Label(text).save(getContext());
      if (ret) {
        // clear EditText
        editText.setText("");
      }
    }
    return ret;
  }
}

