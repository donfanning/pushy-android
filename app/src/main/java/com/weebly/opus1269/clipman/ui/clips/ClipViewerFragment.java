/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.clips;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.util.Linkify;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.databinding.ClipViewerBinding;
import com.weebly.opus1269.clipman.db.entity.Clip;
import com.weebly.opus1269.clipman.db.entity.Label;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.base.BaseFragment;
import com.weebly.opus1269.clipman.ui.help.HelpActivity;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;
import com.weebly.opus1269.clipman.ui.main.MainHandlers;
import com.weebly.opus1269.clipman.ui.settings.SettingsActivity;
import com.weebly.opus1269.clipman.viewmodel.MainViewModel;

import java.text.Collator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A fragment to view a {@link Clip} */
public class ClipViewerFragment extends BaseFragment<ClipViewerBinding> {
  /** Our Option menu */
  private Menu mOptionsMenu = null;

  /** Our ViewModel */
  private MainViewModel mVm = null;

  public ClipViewerFragment() {
    // Required empty public constructor
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

    return mBinding.getRoot();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    final BaseActivity baseActivity = (BaseActivity)getActivity();
    if ((baseActivity == null) || (mBinding == null)) {
      return;
    }

    // setup ViewModel and data binding
    mVm = ViewModelProviders.of(baseActivity).get(MainViewModel.class);
    final MainHandlers handlers = new MainHandlers(baseActivity);
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(mVm);
    mBinding.setHandlers(handlers);
    mBinding.executePendingBindings();

    subscribeToViewModel();

    // TODO
    clipChanged(mVm.getSelClipSync());
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    mOptionsMenu = menu;
    inflater.inflate(R.menu.menu_clipviewer_fragment, menu);
    tintMenuItems();
    updateOptionsMenu();
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final Activity activity = getActivity();
    if (activity == null) {
      return false;
    }

    boolean processed = true;

    Intent intent;
    final int id = item.getItemId();
    switch (id) {
      case R.id.action_favorite:
        mVm.toggleSelFavorite();
        break;
      case R.id.action_labels:
        intent = new Intent(activity, LabelsSelectActivity.class);
        intent.putExtra(Intents.EXTRA_CLIP, getClip());
        AppUtils.startActivity(activity, intent);
        break;
      case R.id.action_edit_text:
        intent = new Intent(activity, ClipEditorActvity.class);
        intent.putExtra(Intents.EXTRA_CLIP, getClip());
        AppUtils.startActivity(activity, intent);
        break;
      case R.id.action_search_web:
        final Clip clip = getClip();
        if (!Clip.isWhitespace(clip)) {
          AppUtils.performWebSearch(activity, clip.getText());
        }
        break;
      case R.id.action_copy:
        mVm.copySelClip();
        break;
      case R.id.action_delete:
        mVm.removeSelClip();
        break;
      case R.id.action_settings:
        intent = new Intent(activity, SettingsActivity.class);
        AppUtils.startActivity(activity, intent);
        break;
      case R.id.action_help:
        intent = new Intent(activity, HelpActivity.class);
        AppUtils.startActivity(activity, intent);
        break;
      default:
        processed = false;
        break;
    }

    if (processed) {
      Analytics.INST(activity).menuClick(TAG, item);
    }

    return processed || super.onOptionsItemSelected(item);
  }

  @Nullable
  private Clip getClip() {
    return mVm == null ? null : mVm.getSelClipSync();
  }

  public void setHighlightText(@NonNull String s) {
    highlightTextChanged(s);
  }

  /** Observe changes to ViewModel */
  private void subscribeToViewModel() {
    // observe clip
    mVm.getSelClip().observe(this, this::clipChanged);

    // observe labels
    mVm.getSelLabels().observe(this, this::labelsChanged);

    // observe clip text filter
    mVm.getClipTextFilter().observe(this, this::highlightTextChanged);
  }

  /**
   * Our Clip changed
   * @param clip The clip
   */
  private void clipChanged(@Nullable Clip clip) {
    if (Clip.isWhitespace(clip)) {
      mBinding.clipViewerText.setVisibility(View.GONE);
      setText("");
    } else {
      final String curText = mBinding.clipViewerText.getText().toString();
      if (!Collator.getInstance().equals(clip.getText(), curText)) {
        // force repaint on text change
        mBinding.clipViewerText.setVisibility(View.GONE);
        mBinding.clipViewerText.setVisibility(View.VISIBLE);
        setText(clip.getText());
      }
    }

    updateOptionsMenu();
    highlightTextChanged(mVm.getClipTextFilterSync());
  }

  /**
   * Highlight all occurrences of the given text
   * @param highlightText Text to highlight
   */
  private void highlightTextChanged(@NonNull String highlightText) {
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
  private void labelsChanged(@Nullable List<Label> labels) {
    final LinearLayout labelList = mBinding.labelList;
    if (labelList.getChildCount() > 0) {
      labelList.removeAllViews();
    }

    // set margins
    final LinearLayout.LayoutParams llp =
      new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT);
    final int rightMargin = AppUtils.dp2px(labelList.getContext(), 8);
    llp.setMargins(0, 0, rightMargin, 0);

    if (!AppUtils.isEmpty(labels)) {
      // add view for each Label
      for (Label label : labels) {
        final ContextThemeWrapper wrapper =
          new ContextThemeWrapper(getContext(), R.style.LabelItemView);
        final TextView textView = new TextView(wrapper, null, 0);
        textView.setText(label.getName());
        textView.setLayoutParams(llp);
        labelList.addView(textView);
      }
    }
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

  /** Color the icons white for all API versions */
  private void tintMenuItems() {
    final Activity activity = getActivity();
    if (activity != null && mOptionsMenu != null) {
      final int color = ContextCompat.getColor(activity, R.color.icons);
      MenuTintHelper.on(mOptionsMenu)
        .setMenuItemIconColor(color)
        .apply(activity);
    }
  }

  /** Set Option Menu items based on current state */
  private void updateOptionsMenu() {
    final Context context = getContext();
    if (context == null || mOptionsMenu == null) {
      return;
    }

    final MenuItem menuItem = mOptionsMenu.findItem(R.id.action_favorite);
    final Clip clip = getClip();
    if ((menuItem != null) && (clip != null)) {
      final boolean isFav = clip.getFav();
      final int colorID = isFav ? R.color.red_500_translucent : R.color.icons;
      final int icon = isFav ? R.drawable.ic_favorite_black_24dp :
        R.drawable.ic_favorite_border_black_24dp;
      menuItem.setIcon(icon);
      final int color = ContextCompat.getColor(getContext(), colorID);
      MenuTintHelper.colorMenuItem(menuItem, color, 255);
    }
  }
}
