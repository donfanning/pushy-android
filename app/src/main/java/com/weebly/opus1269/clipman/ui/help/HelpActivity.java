/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.help;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Email;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;
import com.weebly.opus1269.clipman.ui.views.VectorDrawableTextView;

/** This Activity handles the display of help & feedback */
public class HelpActivity extends BaseActivity {

  /** Version dialog */
  private DialogFragment mDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_help;

    super.onCreate(savedInstanceState);

    final TextView release = findViewById(R.id.docRelease);
    release.setTag(getResources()
      .getString(R.string.help_doc_releas_tag_fmt,
        Prefs.INST(this).getVersionName()));

    // color the TextView icons
    tintLeftDrawables();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mOptionsMenuID = R.menu.menu_help;

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    final int id = item.getItemId();
    switch (id) {
      case R.id.action_view_store:
        AppUtils.showInPlayStore();
        break;
      case R.id.action_version:
        showVersionDialog();
        break;
      case R.id.action_privacy:
        AppUtils.showWebUrl(
          getString(R.string.help_privacy_url));
        break;
      case R.id.action_licenses:
        AppUtils.showWebUrl(
          getString(R.string.help_licenses_url));
        break;
      default:
        processed = false;
        break;
    }

    if (processed) {
      Analytics.INST.menuClick(TAG, item);
    }

    return processed || super.onOptionsItemSelected(item);
  }

  /**
   * Handle click on the Help and feedback items
   * @param v the TextView that was clicked
   */
  public void onItemClicked(View v) {

    final TextView textView = (TextView) v;

    Analytics.INST.click(TAG, textView.getText().toString());

    final int id = v.getId();
    switch (id) {
      case R.id.emailTranslate:
      case R.id.emailGeneral:
        Email.INST.send((String) textView.getTag(), null);
        break;
      case R.id.emailQuestion:
      case R.id.emailBug:
      case R.id.emailFeature:
        final String body = Email.INST.getBody();
        Email.INST.send((String) textView.getTag(), body);
        break;
      case R.id.githubIssue:
      case R.id.docApp:
      case R.id.docFaq:
      case R.id.docRelease:
      case R.id.docSource:
        AppUtils.showWebUrl((String) textView.getTag());
        break;
      case R.id.license:
        mDialog.dismiss();
        break;
      default:
        break;
    }
  }

  /** Show the {@link VersionDialogFragment} */
  private void showVersionDialog() {
    mDialog = new VersionDialogFragment();
    mDialog.show(getSupportFragmentManager(), "VersionDialogFragment");
  }

  /** Color the LeftDrawables for our {@link VectorDrawableTextView} views */
  private void tintLeftDrawables() {

    int color;
    if (Prefs.INST(this).isLightTheme()) {
      color = R.color.deep_teal_500;
    } else {
      color = R.color.deep_teal_200;
    }

    DrawableHelper drawableHelper = DrawableHelper
      .withContext(this)
      .withColor(color)
      .withDrawable(R.drawable.ic_email_black_24dp)
      .tint();
    tintLeftDrawable(drawableHelper, R.id.emailQuestion);
    tintLeftDrawable(drawableHelper, R.id.emailBug);
    tintLeftDrawable(drawableHelper, R.id.emailFeature);
    tintLeftDrawable(drawableHelper, R.id.emailTranslate);
    tintLeftDrawable(drawableHelper, R.id.emailGeneral);

    drawableHelper = DrawableHelper
      .withContext(this)
      .withColor(color)
      .withDrawable(R.drawable.github_circle)
      .tint();
    tintLeftDrawable(drawableHelper, R.id.githubIssue);
    tintLeftDrawable(drawableHelper, R.id.docRelease);
    tintLeftDrawable(drawableHelper, R.id.docSource);

    drawableHelper = DrawableHelper
      .withContext(this)
      .withColor(color)
      .withDrawable(R.drawable.ic_help_black_24dp)
      .tint();
    tintLeftDrawable(drawableHelper, R.id.docApp);
    tintLeftDrawable(drawableHelper, R.id.docFaq);
  }

  /**
   * Color the leftDrawable in a {@link VectorDrawableTextView}
   * @param drawableHelper helper class
   * @param viewId         id of VectorDrawableTextView
   */
  private void tintLeftDrawable(DrawableHelper drawableHelper, int viewId) {
    final TextView textView = findViewById(viewId);
    if (textView != null) {
      Drawable drawable = drawableHelper.get();
      textView.setCompoundDrawablesWithIntrinsicBounds(
        drawable, null, null, null);
    }
  }
}

