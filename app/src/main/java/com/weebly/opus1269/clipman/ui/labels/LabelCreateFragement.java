/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.labels;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.databinding.LabelCreateBinding;
import com.weebly.opus1269.clipman.db.entity.LabelEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.repos.MainRepo;
import com.weebly.opus1269.clipman.ui.base.BaseFragment;
import com.weebly.opus1269.clipman.viewmodel.LabelCreateViewModel;

/** Fragment to Create a new {@link Label} */
public class LabelCreateFragement extends BaseFragment implements
  TextView.OnEditorActionListener {

  /** Our ViewModel */
  private LabelCreateViewModel mVm;

  /** Our DataBinding */
  private LabelCreateBinding mBinding;

  public LabelCreateFragement() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_label_create,
      container, false);
    final View rootView = mBinding.getRoot();

    setup(rootView);

    // setup ViewModel and data binding
    mVm = new LabelCreateViewModel(App.INST());
    final LabelCreateHandlers handlers = new LabelCreateHandlers();
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(mVm);
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

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

  /**
   * Initialize the view
   * @param rootView The View
   */
  private void setup(View rootView) {
    final EditText addText = rootView.findViewById(R.id.addText);

    // listen for text changes and actions
    //addText.addTextChangedListener(this);
    addText.setOnEditorActionListener(this);
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

