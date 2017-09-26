/*
 *
 * Copyright 2016 Michael A Updike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.weebly.opus1269.clipman.model;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weebly.opus1269.clipman.app.App;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Static Class to manage the collection of {@link Label} objects.
 * Register a {@link LocalBroadcastManager} with
 * Intents.FILTER_LABELS to be notified of changes.
 */
public class Labels {

  @SuppressWarnings("StaticNonFinalField")
  private static List<Label> sLabels = load();

  private Labels() {
  }

  /**
   * Save list to persistant storage
   */
  private static void save() {
    final Comparator<Label> cmp = new Comparator<Label>() {
      @Override
      public int compare(Label lhs, Label rhs) {
        // alphabetical
        return lhs.getName().compareTo(rhs.getName());
      }
    };
    // alphabetical
    Collections.sort(sLabels, cmp);

    // persist
    final Gson gson = new Gson();
    final String labelsString = gson.toJson(sLabels);
    Prefs.setLabels(labelsString);
  }

  /**
   * Load list from persistant storage
   * @return the list of {@link Label} objects
   */
  private static List<Label> load() {
    final String labelsString = Prefs.getLabels();

    if (labelsString.isEmpty()) {
      sLabels = new ArrayList<>(0);
    } else {
      final Gson gson = new Gson();
      final Type type = new TypeToken<ArrayList<Label>>() {
      }.getType();
      sLabels = gson.fromJson(labelsString, type);
    }
    return sLabels;
  }

  /**
   * Add {@link Label} Broadcast change if requested.
   * @param lab       The {@link Label} to add or update
   * @param broadcast if true, broadcast result to listeners
   */
  public static void add(Label lab, Boolean broadcast) {
    if ((lab == null) || lab.getName().isEmpty()) {
      return;
    }
    // skip if it exists
    for (final Label label : sLabels) {
      if (lab.getName().equals(label.getName())) {
        return;
      }
    }

    sLabels.add(lab);
    save();
    if (broadcast) {
      _sendBroadcast(Intents.TYPE_LABEL_ADDED, lab);
    }
  }

  /**
   * Remove the given {@link Label}
   * @param lab The {@link Label} to remove
   */
  public static void remove(Label lab) {
    if ((lab == null) || lab.getName().isEmpty()) {
      return;
    }
    for (final Iterator<Label> i = sLabels.iterator(); i.hasNext(); ) {
      final Label label = i.next();
      if (lab.getName().equals(label.getName())) {
        i.remove();
        save();
        _sendBroadcast(Intents.TYPE_LABEL_REMOVED, lab);
        break;
      }
    }
  }

  /**
   * Get the {@link Label} at the given position
   * @param pos Position in list
   * @return A {@link Label}
   */
  public static Label get(int pos) {
    return sLabels.get(pos);
  }

  /**
   * Get the number of {@link Label} objects in the list
   * @return the number of {@link Label} objects in the list
   */
  public static int getCount() {
    return sLabels.size();
  }


  /**
   * Broadcast changes to listeners
   * @param action  the type of the change
   * @param label   Label that changed
   * @param oldname old name of label
   */
  private static void _sendBroadcast(String action, Label label,
                                     String oldname) {
    final Intent intent = new Intent(Intents.FILTER_LABELS);
    final Bundle bundle = new Bundle();
    bundle.putString(Intents.ACTION_TYPE_LABELS, action);
    bundle.putSerializable(Intents.EXTRA_LABEL, label);
    if (TextUtils.isEmpty(oldname)) {
      bundle.putString(Intents.EXTRA_OLD_LABEL_NAME, oldname);
    }
    intent.putExtra(Intents.BUNDLE_LABELS, bundle);
    LocalBroadcastManager
      .getInstance(App.getContext())
      .sendBroadcast(intent);
  }

  /**
   * Broadcast changes to listeners
   * @param action the type of the change
   * @param label  value of extra
   */
  private static void _sendBroadcast(String action, Label label) {
    _sendBroadcast(action, label, null);
  }
}
