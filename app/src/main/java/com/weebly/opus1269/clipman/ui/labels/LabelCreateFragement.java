/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.repos.MainRepo;
import com.weebly.opus1269.clipman.ui.base.BaseFragment;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

import java.util.Arrays;
import java.util.List;

/** Fragment to Create a new {@link Label} */
public class LabelCreateFragement extends BaseFragment implements
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
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    final View rootView =
      inflater.inflate(R.layout.fragment_label_create, container, false);

    setup(rootView);

    return rootView;
  }

  @Override
  public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
    if (textView.getId() == R.id.addText) {
      if (id == EditorInfo.IME_ACTION_DONE) {
        createLabel();
        Analytics.INST(textView.getContext()).keyClick(TAG, "addLabel");
        return true;
      } else if (keyEvent != null) {
        final int keyAction = keyEvent.getAction();
        if (keyAction == KeyEvent.ACTION_DOWN) {
          // eat it
          return true;
        } else if (keyAction == KeyEvent.ACTION_UP) {
          createLabel();
          Analytics.INST(textView.getContext()).keyClick(TAG, "addLabel");
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void onClick(View view) {
    if (view.getId() == R.id.addDoneButton) {
      createLabel();
      Analytics.INST(view.getContext()).imageClick(TAG, "addLabel");
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
    final ImageButton doneButton = findViewById(R.id.addDoneButton);
    final String text = editable.toString();
    final boolean enabled = (TextUtils.getTrimmedLength(text) > 0);
    if (doneButton != null) {
      DrawableHelper.setImageViewEnabled(doneButton, enabled);
    }
  }

  /**
   * Initialize the view
   * @param rootView The View
   */
  private void setup(View rootView) {
    final Context context = rootView.getContext();

    final ImageView addImage = rootView.findViewById(R.id.addImage);
    final EditText addText = rootView.findViewById(R.id.addText);
    final ImageButton addDoneButton = rootView.findViewById(R.id.addDoneButton);

    // listen for clicks
    addDoneButton.setOnClickListener(this);

    // listen for text changes and actions
    addText.addTextChangedListener(this);
    addText.setOnEditorActionListener(this);

    DrawableHelper.setImageViewEnabled(addDoneButton, false);

    final List<ImageView> list = Arrays.asList(addImage, addDoneButton);
    DrawableHelper.tintAccentColor(context, list);
  }

  /** Create a new {@link Label} */
  private void createLabel() {
    final EditText editText = findViewById(R.id.addText);
    assert editText != null;

    String text = editText.getText().toString();
    if (!AppUtils.isWhitespace(text)) {
      text = text.trim();
      // may already exist, but don't really care
      final LabelEntity label = new LabelEntity(text);
      MainRepo.INST(App.INST()).addLabelAsync(label);
      //new Label(text).save(getContext());
      editText.setText("");
    }
  }
}

