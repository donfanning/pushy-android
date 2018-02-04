/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.ClipboardHelper;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.databinding.MainBinding;
import com.weebly.opus1269.clipman.db.entity.ClipEntity;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Intents;
import com.weebly.opus1269.clipman.model.LastError;
import com.weebly.opus1269.clipman.model.Notifications;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.repos.MainRepo;
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.clips.ClipEditorActvity;
import com.weebly.opus1269.clipman.ui.clips.ClipViewerActivity;
import com.weebly.opus1269.clipman.ui.clips.ClipViewerFragment;
import com.weebly.opus1269.clipman.ui.devices.DevicesActivity;
import com.weebly.opus1269.clipman.ui.errorviewer.ErrorViewerActivity;
import com.weebly.opus1269.clipman.ui.help.HelpActivity;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;
import com.weebly.opus1269.clipman.ui.labels.LabelsEditActivity;
import com.weebly.opus1269.clipman.ui.labels.LabelsSelectActivity;
import com.weebly.opus1269.clipman.ui.settings.SettingsActivity;
import com.weebly.opus1269.clipman.ui.signin.SignInActivity;
import com.weebly.opus1269.clipman.viewmodel.MainViewModel;

import java.util.List;

/** Top level Activity for the app */
public class MainActivity extends BaseActivity<MainBinding> implements
  NavigationView.OnNavigationItemSelectedListener,
  View.OnLayoutChangeListener,
  ClipViewerFragment.OnClipChanged,
  DeleteDialogFragment.DeleteDialogListener,
  SharedPreferences.OnSharedPreferenceChangeListener {

  /** ViewModel */
  private MainViewModel mVm = null;

  /** Event handlers */
  private ClipHandlers mHandlers = null;

  /** Adapter used to display the list's data */
  private ClipAdapter mAdapter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mLayoutID = R.layout.activity_main;
    mIsBound = true;

    // We are the big dog, no need for this
    mHomeUpEnabled = false;

    super.onCreate(savedInstanceState);

    final Intent intent = getIntent();
    // peak at intent to see if we need to show a clip notification
    if (intent.hasExtra(Intents.EXTRA_CLIP_ITEM)) {
      final int msgCt = intent.getIntExtra(Intents.EXTRA_CLIP_COUNT, 0);
      if (msgCt > 1) {
        // we will show them, reset LabelFilter
        Prefs.INST(this).setLabelFilter("");
      }
    }

    // listen for preference changes
    PreferenceManager.getDefaultSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(this);

    // setup ViewModel and data binding
    mVm = new MainViewModel(getApplication());
    mHandlers = new ClipHandlers(this);
    mBinding.setLifecycleOwner(this);
    mBinding.setVm(mVm);
    mBinding.setHandlers(mHandlers);
    mBinding.executePendingBindings();

    // observe info messages
    mVm.getInfoMessage().observe(this, msg -> {
      if (TextUtils.equals(msg, "Added clip")) {
        // TODO
        AppUtils.showMessage(this, mBinding.fab, msg);
      }
    });

    // observe error messages
    mVm.getErrorMsg().observe(this, errorMsg -> {
      if (errorMsg != null) {
        AppUtils.showMessage(this, mBinding.fab, errorMsg.msg);
      }
    });

    // observe selected clip
    mVm.getSelectedClip().observe(this, clip -> {
      if (!ClipEntity.isWhitespace(clip)) {
        Log.logD(TAG, "selected clip changed: " + clip.getId());
        int pos = -1;
        final List<ClipEntity> clips = mVm.getClipsSync();
        if (!AppUtils.isEmpty(clips)) {
          for (int i = 0; i < clips.size(); i++) {
            if (clips.get(i).getText().equals(clip.getText())) {
              pos = i;
              break;
            }
          }
        }
        setSelectedClipPos(pos);
        setFabVisibility(!ClipEntity.isWhitespace(clip));
        setTitle();
      }
    });

    // observe undo clips
    mVm.undoClips.observe(this, clips -> {
      if (AppUtils.isEmpty(clips)) {
        return;
      }
      final int nRows = clips.size();
      String message = nRows + getString(R.string.items_deleted);

      switch (nRows) {
        case 0:
          message = getString(R.string.item_delete_empty);
          break;
        case 1:
          message = getString(R.string.item_deleted_one);
          break;
        default:
          break;
      }
      final Snackbar snack =
        Snackbar.make(mBinding.fab, message, 10000);
      if (nRows > 0) {
        snack.setAction(R.string.button_undo, v -> {
          final Context ctxt = v.getContext();
          Analytics.INST(ctxt)
            .imageClick(TAG, ctxt.getString(R.string.button_undo));
          MainRepo.INST(App.INST()).addClips(clips);
        }).addCallback(new Snackbar.Callback() {

          @Override
          public void onShown(Snackbar snackbar) {
          }

          @Override
          public void onDismissed(Snackbar snackbar, int event) {
            mVm.undoClips.setValue(null);
          }
        });
      }
      snack.show();
    });

    final RecyclerView recyclerView = findViewById(R.id.clipList);
    mAdapter = new ClipAdapter(this, mHandlers);
    //binding.contentBackupLayout.backupListLayout.backupRecyclerView
    //  .setAdapter(mAdapter);
    recyclerView.setAdapter(mAdapter);

    // handle touch events on the RecyclerView
    final ItemTouchHelper.Callback callback =
      new ClipItemTouchHelper(this);
    ItemTouchHelper helper = new ItemTouchHelper(callback);
    helper.attachToRecyclerView(recyclerView);

    // Observe clips
    mVm.getClips().observe(this, clips -> {
      mAdapter.setList(clips);
      if (!AppUtils.isEmpty(clips)) {
        if (AppUtils.isDualPane(this) && mVm.selectedPos == -1) {
          setSelectedClipPos(0);
          startOrUpdateClipViewer(clips.get(0));
        }
      } else {
        setSelectedClipPos(-1);
        //startOrUpdateClipViewer(new ClipEntity());
      }
    });

    setupNavigationView();

    if (AppUtils.isDualPane(this)) {
      // create the clip viewer for the two pane option
      final ClipViewerFragment fragment =
        ClipViewerFragment.newInstance(new ClipEntity(), "");
      getSupportFragmentManager().beginTransaction()
        .replace(R.id.clip_viewer_container, fragment)
        .commit();
    }

    setFabVisibility(false);

    final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
      mBinding.drawerLayout, mBinding.toolbar, R.string.navigation_drawer_open,
      R.string.navigation_drawer_close);
    mBinding.drawerLayout.addDrawerListener(toggle);
    toggle.syncState();

    mBinding.navView.setNavigationItemSelectedListener(this);

    handleIntent();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // TODO in case filter changed
    //mVm.labelFilter = Prefs.INST(this).getLabelFilter();

    setTitle();
    updateNavView();
    updateOptionsMenu();
    Notifications.INST(this).removeClips();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    final boolean ret;

    mOptionsMenuID = R.menu.menu_main;

    ret = super.onCreateOptionsMenu(menu);

    updateOptionsMenu();

    return ret;
  }

  @Override
  protected boolean setQueryString(String queryString) {
    boolean ret = false;
    if (super.setQueryString(queryString)) {
      // TODO
      //getSupportLoaderManager().restartLoader(0, null, mLoaderManager);
      ret = true;
    }
    return ret;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    // stop listening for preference changes
    PreferenceManager.getDefaultSharedPreferences(this)
      .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onBackPressed() {
    if (!TextUtils.isEmpty(mVm.labelFilter)) {
      // if filtered, create unfiltered MainActivity on Back
      Prefs.INST(this).setLabelFilter("");
      startActivity(MainActivity.class);
      finish();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    mVm.undoClips.setValue(null);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    Intent intent;
    final int id = item.getItemId();
    switch (id) {
      case R.id.home:
        NavUtils.navigateUpFromSameTask(this);
        setQueryString("");
        break;
      case R.id.action_send:
        ClipboardHelper.sendClipboardContents(this, mBinding.fab);
        break;
      case R.id.action_pin:
        Prefs.INST(this).setPinFav(!mVm.pinFavs);
        updateOptionsMenu();
        break;
      case R.id.action_fav_filter:
        Prefs.INST(this).setFavFilter(!mVm.filterByFavs);
        updateOptionsMenu();
        break;
      case R.id.action_sort:
        final DialogFragment sortDialog = new SortTypeDialogFragment();
        sortDialog.show(getSupportFragmentManager(), "SortTypeDialogFragment");
        break;
      case R.id.action_labels:
        intent = new Intent(this, LabelsSelectActivity.class);
        intent.putExtra(Intents.EXTRA_CLIP, getSelectedClipSync());
        AppUtils.startActivity(this, intent);
        break;
      case R.id.action_add_clip:
        intent = new Intent(this, ClipEditorActvity.class);
        intent.putExtra(Intents.EXTRA_TEXT, mVm.labelFilter);
        AppUtils.startActivity(this, intent);
        break;
      case R.id.action_edit_text:
        intent = new Intent(this, ClipEditorActvity.class);
        intent.putExtra(Intents.EXTRA_CLIP, getSelectedClipSync());
        AppUtils.startActivity(this, intent);
        break;
      case R.id.action_delete:
        final DialogFragment deleteDialog = new DeleteDialogFragment();
        deleteDialog.show(getSupportFragmentManager(), "DeleteDialogFragment");
        break;
      case R.id.action_settings:
        startActivity(SettingsActivity.class);
        break;
      case R.id.action_help:
        startActivity(HelpActivity.class);
        break;
      default:
        processed = false;
        break;
    }

    if (processed) {
      Analytics.INST(this).menuClick(TAG, item);
    }

    return processed || super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    boolean processed = true;
    final int id = item.getItemId();

    switch (id) {
      case R.id.nav_account:
        startActivity(SignInActivity.class);
        break;
      case R.id.nav_backup:
        startActivity(BackupActivity.class);
        break;
      case R.id.nav_devices:
        startActivity(DevicesActivity.class);
        break;
      case R.id.nav_settings:
        startActivity(SettingsActivity.class);
        break;
      case R.id.nav_labels_edit:
        startActivity(LabelsEditActivity.class);
        break;
      case R.id.nav_clips:
        if (!AppUtils.isWhitespace(mVm.labelFilter)) {
          Prefs.INST(this).setLabelFilter("");
          startActivity(MainActivity.class);
          finish();
        }
        break;
      case Menu.NONE:
        // all Labels items
        final String label = item.getTitle().toString();
        if (!label.equals(mVm.labelFilter)) {
          Prefs.INST(this).setLabelFilter(label);
          startActivity(MainActivity.class);
          finish();
        }
        break;
      case R.id.nav_error:
        startActivity(ErrorViewerActivity.class);
        break;
      case R.id.nav_help:
        startActivity(HelpActivity.class);
        break;
      case R.id.nav_chrome_extension:
        AppUtils.showWebUrl(this, getString(R.string.chrome_extension_url));
        break;
      case R.id.rate_app:
        AppUtils.showInPlayStore(this);
        break;
      default:
        processed = false;
        break;
    }

    if (processed) {
      Analytics.INST(this).menuClick(TAG, item);
    }

    mBinding.drawerLayout.closeDrawer(GravityCompat.START);

    return true;
  }

  /** Set NavigationView header aspect ratio to 16:9 */
  @Override
  public void onLayoutChange(View v, int left, int top, int right, int bottom,
                             int oldLeft, int oldTop, int oldRight,
                             int oldBottom) {
    if (v.equals(mBinding.navView)) {
      final int oldWidth = oldRight - oldLeft;
      final int width = right - left;
      final View hView = mBinding.navView.getHeaderView(0);
      if ((hView != null) && (oldWidth != width)) {
        hView.getLayoutParams().height = Math.round((9.0F / 16.0F) * width);
      }
    }
  }

  @Override
  public void clipChanged(ClipEntity clip) {
    mVm.setSelectedClip(clip);
  }

  @Override
  public void onDeleteDialogPositiveClick(Boolean includeFavs) {
    mVm.removeAll(includeFavs);
  }

  @Override
  public void
  onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    final String keyPush = getString(R.string.key_pref_push_msg);

    if (LastError.PREF_LAST_ERROR.equals(key)) {
      updateNavView();
    } else if (keyPush.equals(key)) {
      updateOptionsMenu();
    }
  }

  public int getSelectedClipPos() {
    return mVm.selectedPos;
  }

  public void setSelectedClipPos(int position) {
    if (mVm.selectedPos == position) {
      return;
    }

    if (position < 0) {
      mVm.selectedPos = -1;
    } else {
      if (mVm.selectedPos >= 0) {
        mAdapter.notifyItemChanged(mVm.selectedPos);
      }
      mVm.selectedPos = position;
      mAdapter.notifyItemChanged(mVm.selectedPos);
    }
  }

  public MainViewModel getVm() {
    return mVm;
  }

  /**
   * Start {@link ClipViewerActivity} or update the {@link ClipViewerFragment}
   * @param clip The Clip to view
   */
  void startOrUpdateClipViewer(ClipEntity clip) {
    if (AppUtils.isDualPane(this)) {
      final ClipViewerFragment fragment =
        (ClipViewerFragment) getSupportFragmentManager()
          .findFragmentById(R.id.clip_viewer_container);
      if (fragment != null) {
        fragment.setClip(clip);
        fragment.setHighlight(mQueryString);
      }
    } else {
      final Intent intent = new Intent(this, ClipViewerActivity.class);
      intent.putExtra(Intents.EXTRA_CLIP, clip);
      intent.putExtra(Intents.EXTRA_TEXT, mQueryString);
      AppUtils.startActivity(this, intent);
    }
  }

  /** Process intents we know about */
  private void handleIntent() {
    final Intent intent = getIntent();
    final String action = intent.getAction();
    final String type = intent.getType();

    if (Intent.ACTION_SEND.equals(action) && (type != null)) {
      // Shared from other app
      if (ClipEntity.TEXT_PLAIN.equals(type)) {
        final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
          final ClipEntity clip = new ClipEntity();
          clip.setText(sharedText);
          // TODO
          MainRepo.INST(App.INST()).addClip(clip);
        }
      }
    } else if (intent.hasExtra(Intents.EXTRA_CLIP)) {
      // from clip notification
      final int msgCt = intent.getIntExtra(Intents.EXTRA_CLIP_COUNT, 0);
      if (msgCt == 1) {
        // if 1 message open Clipviewer, otherwise show in us
        final ClipEntity clip =
          (ClipEntity) intent.getSerializableExtra(Intents.EXTRA_CLIP);
        intent.removeExtra(Intents.EXTRA_CLIP);
        startOrUpdateClipViewer(clip);
      }
    }
  }

  private @Nullable
  ClipEntity getSelectedClipSync() {
    return mVm == null ? null : mVm.getSelectedClipSync();
  }

  /**
   * Start an {@link Activity}
   * @param cls Class of Activity
   */
  private void startActivity(Class cls) {
    final Intent intent = new Intent(this, cls);
    AppUtils.startActivity(this, intent);
  }

  /** Set title based on currently selected {@link ClipEntity} */
  private void setTitle() {
    String prefix = getString(R.string.title_activity_main);
    if (!AppUtils.isWhitespace(mVm.labelFilter)) {
      prefix = mVm.labelFilter;
    }
    if (AppUtils.isDualPane(this)) {
      final ClipEntity clip = getSelectedClipSync();
      if (clip != null && clip.getRemote()) {
        setTitle(getString(R.string.title_activity_main_remote_fmt, prefix,
          clip.getDevice()));
      } else {
        setTitle(getString(R.string.title_activity_main_local_fmt, prefix));
      }
    } else {
      setTitle(prefix);
    }
  }

  /** Initialize the NavigationView */
  private void setupNavigationView() {
    mBinding.navView.addOnLayoutChangeListener(this);

    // Handle click on header
    final View hView = mBinding.navView.getHeaderView(0);
    hView.setOnClickListener(v -> {
      mBinding.drawerLayout.closeDrawer(GravityCompat.START);
      startActivity(SignInActivity.class);
    });
  }

  /** Update the Navigation View */
  private void updateNavView() {
    final View hView = mBinding.navView.getHeaderView(0);
    if (hView == null) {
      return;
    }

    final Menu menu = mBinding.navView.getMenu();

    // set BackupHelper menu state
    MenuItem menuItem = menu.findItem(R.id.nav_backup);
    menuItem.setEnabled(User.INST(this).isLoggedIn());

    // set Devices menu state
    menuItem = menu.findItem(R.id.nav_devices);
    menuItem.setEnabled(User.INST(this).isLoggedIn());

    // set Error Viewer menu state
    menuItem = menu.findItem(R.id.nav_error);
    menuItem.setEnabled(LastError.exists(this));

    // Create Labels sub menu
    // TODO
    //List<Label> labels = LabelTables.INST(this).getAllLabels();
    //menu.setGroupVisible(R.id.nav_group_labels, !AppUtils.isEmpty(labels));
    //SubMenu labelMenu = menu.findItem(R.id.nav_labels_sub_menu).getSubMenu();
    //labelMenu.clear();
    //for (Label label : labels) {
    //  final MenuItem labelItem = labelMenu.add(R.id.nav_group_labels,
    //    Menu.NONE, Menu.NONE, label.getName());
    //  labelItem.setIcon(R.drawable.ic_label);
    //}

    User.INST(this).setNavigationHeaderView(hView);
  }

  /** Set Option Menu items based on current state */
  private void updateOptionsMenu() {
    if (mOptionsMenu != null) {

      if (!AppUtils.isDualPane(this)) {
        // hide labels and edit_text menu if not dual pane
        MenuItem menuItem;
        menuItem = mOptionsMenu.findItem(R.id.action_labels);
        if (menuItem != null) {
          menuItem.setVisible(false);
        }
        menuItem = mOptionsMenu.findItem(R.id.action_edit_text);
        if (menuItem != null) {
          menuItem.setVisible(false);
        }
      }

      // enabled state of send button
      Boolean enabled = false;
      Integer alpha = 64;
      if (User.INST(this).isLoggedIn() && Prefs.INST(this).isPushClipboard()) {
        enabled = true;
        alpha = 255;
      }
      final MenuItem sendItem = mOptionsMenu.findItem(R.id.action_send);
      MenuTintHelper.colorMenuItem(sendItem, null, alpha);
      sendItem.setEnabled(enabled);

      // pin fav state
      final MenuItem pinMenu =
        mOptionsMenu.findItem(R.id.action_pin);
      if (mVm.pinFavs) {
        pinMenu.setIcon(R.drawable.ic_pin);
        pinMenu.setTitle(R.string.action_no_pin);
      } else {
        pinMenu.setIcon(R.drawable.ic_no_pin);
        pinMenu.setTitle(R.string.action_pin);
      }
      final int pinColor = ContextCompat.getColor(this, R.color.icons);
      MenuTintHelper.colorMenuItem(pinMenu, pinColor, 255);

      // fav filter state
      final MenuItem favFilterMenu =
        mOptionsMenu.findItem(R.id.action_fav_filter);
      int colorID;
      if (mVm.filterByFavs) {
        favFilterMenu.setIcon(R.drawable.ic_favorite_black_24dp);
        favFilterMenu.setTitle(R.string.action_show_all);
        colorID = R.color.red_500_translucent;
      } else {
        favFilterMenu.setIcon(R.drawable.ic_favorite_border_black_24dp);
        favFilterMenu.setTitle(R.string.action_show_favs);
        colorID = R.color.icons;
      }
      final int favFilterColor = ContextCompat.getColor(this, colorID);
      MenuTintHelper.colorMenuItem(favFilterMenu, favFilterColor, 255);
    }
  }
}
