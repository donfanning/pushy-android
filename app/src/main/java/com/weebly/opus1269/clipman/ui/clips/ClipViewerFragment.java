/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clips;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.databinding.ClipViewerBinding;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.base.BaseFragment;
import com.weebly.opus1269.clipman.viewmodel.ClipViewerFragViewModel;

import java.io.Serializable;
import java.text.Collator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A fragment to view a {@link ClipEntity} */
public class ClipViewerFragment extends BaseFragment<ClipViewerBinding> {
  /** The Activity that has implemented our interface */
  private OnClipChanged mOnClipChanged = null;

  /** Our ViewModel */
  private ClipViewerFragViewModel mVm = null;

  public ClipViewerFragment() {
    // Required empty public constructor
  }

  /**
   * Factory method to create new fragment
   * @param item      ClipEntity to view
   * @param highlight text to highlight
   * @return new ClipViewerFragment
   */
  public static ClipViewerFragment newInstance(Serializable item,
                                               String highlight) {
    final ClipViewerFragment fragment = new ClipViewerFragment();

    final Bundle args = new Bundle();
    args.putSerializable(Intents.EXTRA_CLIP, item);
    args.putString(Intents.EXTRA_TEXT, highlight);

    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {

    if (container == null) {
      // We have different layouts, and in one of them this
      // fragment's containing frame doesn't exist.  The fragment
      // may still be created from its saved state, but there is
      // no reason to try to create its view hierarchy because it
      // won't be displayed.  Note this is not needed -- we could
      // just run the code below, where we would create and return
      // the view hierarchy; it would just never be used.
      setHasOptionsMenu(false);
      return null;
    }

    mLayoutID = R.layout.fragment_clip_viewer;
    mIsBound = true;

    super.onCreateView(inflater, container, savedInstanceState);

    // setup ViewModel and data binding
    mVm = new ClipViewerFragViewModel(App.INST());
    final ClipViewerHandlers handlers = new ClipViewerHandlers();
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(mVm);
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

    // observe clip
    mVm.getClip().observe(this, clipEntity -> {
      if (clipEntity != null) {
        clipChanged(clipEntity);
      }
    });

    return mBinding.getRoot();
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    final Activity activity = getActivity();
    // This makes sure that the container activity has implemented
    // the callback interface. If not, it throws an exception
    try {
      mOnClipChanged = (OnClipChanged) activity;
    } catch (final ClassCastException ignore) {
      throw new ClassCastException(activity.getLocalClassName() +
        " must implement OnClipChanged");
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    final Bundle args = getArguments();
    if (args != null) {
      if (args.containsKey(Intents.EXTRA_CLIP)) {
        final ClipEntity clip =
          (ClipEntity) args.getSerializable(Intents.EXTRA_CLIP);
        if (mVm != null) {
          mVm.setClip(clip);
        }
      }

      if (args.containsKey(Intents.EXTRA_TEXT)) {
        if (mVm != null) {
          mVm.setHighlight(args.getString(Intents.EXTRA_TEXT));
        }
      }
    }
  }

  /** Get our Clip */
  public @Nullable
  ClipEntity getClip() {
    return mVm.getClipSync();
  }

  public void setClip(ClipEntity clip) {
    mVm.setClip(clip);
  }

  public void setHighlight(String s) {
    mVm.setHighlight(s);
  }

  /**
   * Our Clip changed
   * @param clip The clip
   */
  private void clipChanged(@NonNull ClipEntity clip) {
    if (ClipEntity.isWhitespace(clip)) {
      return;
    }

    Log.logD(TAG, "clip changed\n" + clip.toString());

    final String curText = mBinding.clipViewerText.getText().toString();
    if (!Collator.getInstance().equals(clip.getText(), curText)) {
      // force repaint on text change
      mBinding.clipViewerText.setVisibility(View.GONE);
      mBinding.clipViewerText.setVisibility(View.VISIBLE);
      setText(clip.getText());
    }

    setupLabels();
    setupHighlight();

    mOnClipChanged.clipChanged(clip);
  }

  /** Highlight all occurrences of the highlight */
  private void setupHighlight() {
    final String highlightText = mVm.getHighlight();
    final Context context = mBinding.clipViewerText.getContext();
    final String text = mBinding.clipViewerText.getText().toString();
    if (TextUtils.isEmpty(highlightText)) {
      // make sure to reset spans
      setText(text);
    } else {
      final Spannable spanText =
        Spannable.Factory.getInstance().newSpannable(text);
      final int color =
        ContextCompat.getColor(context, R.color.search_highlight);
      final Pattern p =
        Pattern.compile(highlightText, Pattern.CASE_INSENSITIVE);
      final Matcher m = p.matcher(text);
      final int length = highlightText.length();
      while (m.find()) {
        final int start = m.start();
        final int stop = start + length;
        spanText.setSpan(new BackgroundColorSpan(color), start, stop,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }

      setText(spanText);
    }
  }

  /** Add the Views containing our {@link Label} items */
  private void setupLabels() {
    // TODO
    //mClip.loadLabels(getContext());
    //final List<Label> labels = mClip.getLabels();
    //
    //final LinearLayout labelLayout = findViewById(R.id.labelLayout);
    //assert labelLayout != null;
    //if (!AppUtils.isEmpty(labels)) {
    //  labelLayout.setVisibility(View.VISIBLE);
    //} else {
    //  labelLayout.setVisibility(View.GONE);
    //  return;
    //}
    //
    //final LinearLayout labelList = findViewById(R.id.labelList);
    //assert labelList != null;
    //if (labelList.getChildCount() > 0) {
    //  labelList.removeAllViews();
    //}
    //
    //// to set margins
    //final LinearLayout.LayoutParams llp =
    //  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
    //    LinearLayout.LayoutParams.WRAP_CONTENT);
    //// llp.setMargins(left, top, right, bottom);
    //final int rightMargin = AppUtils.dp2px(labelLayout.getContext(), 8);
    //llp.setMargins(0, 0, rightMargin, 0);
    //
    //for (Label label : labels) {
    //  final ContextThemeWrapper wrapper =
    //    new ContextThemeWrapper(getContext(), R.style.LabelItemView);
    //  final TextView textView = new TextView(wrapper, null, 0);
    //  textView.setText(label.getName());
    //  textView.setLayoutParams(llp);
    //  labelList.addView(textView);
    //}
  }

  /**
   * Set the TextView text
   * @param text text to be linkified
   */
  private void setText(String text) {
    mBinding.clipViewerText.setText(text);
    linkifyTextView(mBinding.clipViewerText);
  }

  /**
   * Set the TextView text
   * @param text text as Spannable
   */
  private void setText(Spannable text) {
    mBinding.clipViewerText.setText(text);
    linkifyTextView(mBinding.clipViewerText);
  }

  /**
   * Setup selectable links for TextView. AutoLink is pretty buggy
   * @param textView a text view
   */
  private void linkifyTextView(TextView textView) {
    // http://stackoverflow.com/a/16003280/4468645
    try {
      Linkify.addLinks(textView, Linkify.ALL);
    } catch (Exception ex) {
      Log.logEx(textView.getContext(), TAG, ex.getLocalizedMessage(), ex,
        false);
    }
    textView.setMovementMethod(ArrowKeyMovementMethod.getInstance());
    textView.setTextIsSelectable(true);
  }

  /** Activities implement this to get notified of clip changes */
  public interface OnClipChanged {
    void clipChanged(ClipEntity clip);
  }
}
