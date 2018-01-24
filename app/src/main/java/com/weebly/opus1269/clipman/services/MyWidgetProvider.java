/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.services;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.ClipboardHelper;

/**
 * Manage our app's widget
 */
public class MyWidgetProvider extends AppWidgetProvider {

  private static final String ACTION_CLICK = "ACTION_CLICK";

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                       int[] appWidgetIds) {
    // Get all ids
    ComponentName thisWidget = new ComponentName(context,
      MyWidgetProvider.class);
    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
    for (int widgetId : allWidgetIds) {

      RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
        R.layout.widget_layout);

      // Register an onClickListener
      Intent intent = new Intent(context, MyWidgetProvider.class);
      intent.setAction(ACTION_CLICK);
      PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      remoteViews.setOnClickPendingIntent(R.id.sendButton, pendingIntent);

      appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);

    if (ACTION_CLICK.equals(intent.getAction())) {
      ClipboardHelper.sendClipboardContents(context, null);
    }
  }
}
