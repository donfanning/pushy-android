/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clips;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.util.Linkify;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;

import java.io.Serializable;
import java.text.Collator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A fragment to view a {@link ClipItem} */
public class ClipViewerFragment extends Fragment
  implements View.OnClickListener {

  private static final String TAG = "ClipViewerFragment";

  // saved state
  private static final String STATE_CLIP_ITEM = "clip";
  private static final String STATE_CLIP_VIEWABLE = "viewable";
  private static final String STATE_CLIP_HIGHLIGHT = "highlight";

  /** The Activity that has implemented our interface */
  private OnClipChanged mOnClipChanged = null;

  /** The clip we are viewing */
  private ClipItem mClipItem = null;

  /** The text to be highlighted */
  private String mHighlightText = "";

  /**
   * Flag to indicate if we are viewable (as opposed to recreated from a
   * savedInstanceState but will not be seen.
   */
  private boolean mIsViewable = true;

  /** Receive {@link ClipItem} actions */
  private BroadcastReceiver mClipItemReceiver = null;

  /**
   * Factory method to create new fragment
   * @param item      ClipItem to view
   * @param highlight text to highlight
   * @return new ClipViewerFragment
   */
  public static ClipViewerFragment newInstance(Serializable item,
                                               String highlight) {
    final ClipViewerFragment fragment = new ClipViewerFragment();

    final Bundle args = new Bundle();
    args.putSerializable(Intents.EXTRA_CLIP_ITEM, item);
    args.putString(Intents.EXTRA_TEXT, highlight);

    fragment.setArguments(args);

    return fragment;
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

    // Check whether we're recreating a previously destroyed instance
    if (savedInstanceState != null) {
      mClipItem = (ClipItem) savedInstanceState.getSerializable
        (STATE_CLIP_ITEM);
      mIsViewable = savedInstanceState.getBoolean(STATE_CLIP_VIEWABLE);
      mHighlightText = savedInstanceState.getString(STATE_CLIP_HIGHLIGHT);
    } else {
      mClipItem = new ClipItem(getContext());
      mHighlightText = "";
      mIsViewable = true;
    }

    mOnClipChanged.clipChanged(mClipItem);

    // listen for changes to ClipItems
    mClipItemReceiver = new ClipItemReceiver();
    final Context context = getContext();
    if (context != null) {
      LocalBroadcastManager.getInstance(context)
        .registerReceiver(mClipItemReceiver,
          new IntentFilter(Intents.FILTER_CLIP_ITEM));
    }
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
      mIsViewable = false;
      setHasOptionsMenu(false);
      return null;
    }

    final View rootView =
      inflater.inflate(R.layout.fragment_clip_viewer, container, false);

    setText(mClipItem.getText());

    return rootView;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    if (getArguments().containsKey(Intents.EXTRA_CLIP_ITEM)) {
      final ClipItem clipItem =
        (ClipItem) getArguments().getSerializable(Intents.EXTRA_CLIP_ITEM);
      setClipItem(clipItem);
    }

    if (getArguments().containsKey(Intents.EXTRA_TEXT)) {
      final String highlightText = getArguments().getString(Intents.EXTRA_TEXT);
      setHighlightText(highlightText);
    }

    final Activity activity = getActivity();

    final FloatingActionButton fab = activity.findViewById(R.id.fab);
    if (fab != null) {
      fab.setOnClickListener(this);
    }

    final LinearLayout labelList = activity.findViewById(R.id.labelList);
    if (labelList != null) {
      labelList.setOnClickListener(this);
    }
  }

  @Override
  public void onStart() {
    super.onStart();

  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    final Context context = getContext();
    if (context != null) {
      LocalBroadcastManager.getInstance(context)
        .unregisterReceiver(mClipItemReceiver);
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    setupLabels();

    setupRemoteDevice();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putSerializable(STATE_CLIP_ITEM, mClipItem);
    outState.putString(STATE_CLIP_HIGHLIGHT, mHighlightText);
    outState.putBoolean(STATE_CLIP_VIEWABLE, mIsViewable);

    super.onSaveInstanceState(outState);
  }

  @Override
  public void onClick(View v) {
    final Activity activity = getActivity();
    final View fab = activity.findViewById(R.id.fab);
    final View labelList = activity.findViewById(R.id.labelList);
    final String TAG = ((BaseActivity) activity).getTAG();
    if (v == fab) {
      mClipItem.doShare(getContext(), v);
      Analytics.INST(activity).imageClick(TAG, "shareClipItem");
    } else if (v == labelList) {
      Analytics.INST(activity).click(TAG, "showLabelList");
      final Intent intent = new Intent(activity, LabelsSelectActivity.class);
      intent.putExtra(Intents.EXTRA_CLIP_ITEM, mClipItem);
      AppUtils.startActivity(activity, intent);
    }
  }

  /** Get a shallow copy of our ClipItem */
  public @Nullable
  ClipItem getClipItemClone() {
    if (mClipItem != null) {
      return new ClipItem(getContext(), mClipItem);
    } else {
      return null;
    }
  }

  /**
   * Set our ClipItem
   * @param clipItem a ClipItem
   */
  public void setClipItem(ClipItem clipItem) {
    if (!Collator.getInstance().equals(clipItem.getText(), mClipItem.getText
      ())) {
      // skip repaint if text is same
      final TextView textView = getActivity().findViewById(R.id.clipViewerText);
      if (textView != null) {
        //force layout change animation
        textView.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);
        setText(clipItem.getText());
      }
    }

    mClipItem = clipItem;

    setupLabels();

    setupRemoteDevice();

    mOnClipChanged.clipChanged(mClipItem);
  }

  /** Copy our ClipItem to the Clipboard */
  void copyToClipboard() {
    if (!ClipItem.isWhitespace(mClipItem)) {
      final Context context = getContext();

      mClipItem.setRemote(false);
      setupRemoteDevice();

      // let listeners know
      mOnClipChanged.clipChanged(mClipItem);

      // copy and let user know
      mClipItem.copyToClipboard(context);
      View view = getView();
      AppUtils.showMessage(context, view, getString(R.string.clipboard_copy));
    }
  }

  /**
   * Highlight all occurrences of the given String
   * @param highlightText the text to highlight (case insensitive)
   */
  public void setHighlightText(String highlightText) {
    mHighlightText = highlightText;

    final TextView textView = getActivity().findViewById(R.id.clipViewerText);
    if (textView == null) {
      return;
    }

    if (TextUtils.isEmpty(highlightText)) {
      // make sure to reset spans
      setText(mClipItem.getText());
    } else {
      final String text = mClipItem.getText();
      final Spannable spanText =
        Spannable.Factory.getInstance().newSpannable(text);
      final int color =
        ContextCompat.getColor(getContext(), R.color.search_highlight);
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

  /** Set the source view if we are from a remote device or hide it if local */
  private void setupRemoteDevice() {
    if (!mIsViewable) {
      return;
    }

    final TextView textView = getActivity().findViewById(R.id.remoteDevice);
    final View divider = getActivity().findViewById(R.id.remoteDivider);

    if (mClipItem.isRemote() && !AppUtils.isDualPane(getContext())) {
      textView.setVisibility(View.VISIBLE);
      divider.setVisibility(View.VISIBLE);
      textView.setText(getString(R.string.remote_device_fmt, mClipItem
        .getDevice()));
    } else {
      textView.setVisibility(View.GONE);
      divider.setVisibility(View.GONE);
    }
  }

  /** Add the Views containing our {@link Label} items */
  private void setupLabels() {
    if (!mIsViewable) {
      return;
    }

    mClipItem.loadLabels(getContext());
    final List<Label> labels = mClipItem.getLabels();

    final LinearLayout labelLayout =
      getActivity().findViewById(R.id.labelLayout);
    if (labels.size() > 0) {
      labelLayout.setVisibility(View.VISIBLE);
    } else {
      labelLayout.setVisibility(View.GONE);
      return;
    }

    final LinearLayout labelList = getActivity().findViewById(R.id.labelList);
    if (labelList.getChildCount() > 0) {
      labelList.removeAllViews();
    }

    // to set margins
    final LinearLayout.LayoutParams llp =
      new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT);
    // llp.setMargins(left, top, right, bottom);
    final int rightMargin = AppUtils.dp2px(getContext(), 8);
    llp.setMargins(0, 0, rightMargin, 0);

    for (Label label : labels) {
      final ContextThemeWrapper wrapper =
        new ContextThemeWrapper(getContext(), R.style.LabelItemView);
      final TextView textView = new TextView(wrapper, null, 0);
      textView.setText(label.getName());
      textView.setLayoutParams(llp);
      labelList.addView(textView);
    }
  }

  /**
   * Set the TextView text
   * @param text text to be linkified
   */
  private void setText(String text) {
    final TextView textView =
      getActivity().findViewById(R.id.clipViewerText);
    if (textView == null) {
      return;
    }

    textView.setText(text);
    linkifyTextView(textView);
  }

  /**
   * Set the TextView text
   * @param text text as Spannable
   */
  private void setText(Spannable text) {
    final TextView textView =
      getActivity().findViewById(R.id.clipViewerText);
    if (textView == null) {
      return;
    }

    textView.setText(text);
    linkifyTextView(textView);
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
      Log.logEx(getContext(), TAG, ex.getLocalizedMessage(), ex, false);
    }
    textView.setMovementMethod(ArrowKeyMovementMethod.getInstance());
    textView.setTextIsSelectable(true);
  }

  /** Activities implement this to get notified of clip changes */
  public interface OnClipChanged {
    void clipChanged(ClipItem clipItem);
  }


  /** {@link BroadcastReceiver} to handle {@link ClipItem} actions */
  class ClipItemReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      final Bundle bundle = intent.getBundleExtra(Intents.BUNDLE_CLIP_ITEM);
      if (bundle == null) {
        return;
      }
      final String action = bundle.getString(Intents.ACTION_TYPE_CLIP_ITEM);
      if (action == null) {
        return;
      }

      switch (action) {
        case Intents.TYPE_TEXT_CHANGED_CLIP_ITEM:
          Log.logD(TAG, "text changed");
          // text changed on a clip, see if it is us
          final String oldText = bundle.getString(Intents.EXTRA_TEXT);
          if ((oldText!= null) && oldText.equals(mClipItem.getText())) {
            // us
            final ClipItem clipItem =
              (ClipItem) bundle.getSerializable(Intents.EXTRA_CLIP_ITEM);
            setClipItem(clipItem);
          }
          break;
        default:
          break;
      }
    }
  }
}
