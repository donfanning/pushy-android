/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.db;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;

/** App private {@link ContentProvider} for the Clips.db */
public class ClipsContentProvider extends ContentProvider {
  private static final String TAG = "ClipsContentProvider";

  private static final String UNKNOWN_URI = "Unknown URI: ";
  private static final String UNSUPPORTED_URI = "Unsupported URI: ";

  // used for the UriMatcher
  private static final int CLIP = 10;
  private static final int CLIP_ID = 20;
  private static final int LABEL = 30;
  private static final int LABEL_ID = 40;
  private static final int LABEL_MAP = 50;
  private static final int LABEL_MAP_ID = 60;
  private static final int CLIP_LABEL_MAP_JOIN = 70;
  private static final UriMatcher URI_MATCHER =
    new UriMatcher(UriMatcher.NO_MATCH);

  static {
    URI_MATCHER.addURI(ClipsContract.AUTHORITY, "clip", CLIP);
    URI_MATCHER.addURI(ClipsContract.AUTHORITY, "clip/#", CLIP_ID);
    URI_MATCHER.addURI(ClipsContract.AUTHORITY, "label", LABEL);
    URI_MATCHER.addURI(ClipsContract.AUTHORITY, "label/#", LABEL_ID);
    URI_MATCHER.addURI(ClipsContract.AUTHORITY, "label_map", LABEL_MAP);
    URI_MATCHER.addURI(ClipsContract.AUTHORITY, "label_map/#", LABEL_MAP_ID);
    URI_MATCHER.addURI(ClipsContract.AUTHORITY, "clip_label_map_join",
      CLIP_LABEL_MAP_JOIN);
  }

  /** Context we are running in */
  private Context mContext;

  @Override
  public boolean onCreate() {
    mContext = getContext();
    return true;
  }

  @Override
  public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                      String[] selectionArgs, String sortOrder) {

    Uri newUri = uri;
    String newSelection = selection;
    String newSortOrder = sortOrder;
    final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

    final int uriType = URI_MATCHER.match(uri);
    switch (uriType) {
      case CLIP:
        queryBuilder.setTables(ClipsContract.Clip.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipsContract.Clip.getDefaultSortOrder();
        }
        break;
      case LABEL:
        queryBuilder.setTables(ClipsContract.Label.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipsContract.Label.getDefaultSortOrder();
        }
        break;
      case LABEL_MAP:
        queryBuilder.setTables(ClipsContract.LabelMap.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipsContract.LabelMap.getDefaultSortOrder();
        }
        break;
      case CLIP_ID:
        queryBuilder.setTables(ClipsContract.Clip.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipsContract.Clip.getDefaultSortOrder();
        }
        // Because this URI was for a single row, the _ID value part is
        // present. Get the last path segment from the URI; this is the
        // _ID value. Then, append the value to the WHERE clause for
        // the query
        newSelection += "and _ID = " + uri.getLastPathSegment();
        break;
      case LABEL_ID:
        queryBuilder.setTables(ClipsContract.Label.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipsContract.Label.getDefaultSortOrder();
        }
        // Because this URI was for a single row, the _ID value part is
        // present. Get the last path segment from the URI; this is the
        // _ID value. Then, append the value to the WHERE clause for
        // the query
        newSelection += "and _ID = " + uri.getLastPathSegment();
        break;
      case LABEL_MAP_ID:
        queryBuilder.setTables(ClipsContract.LabelMap.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipsContract.LabelMap.getDefaultSortOrder();
        }
        // Because this URI was for a single row, the _ID value part is
        // present. Get the last path segment from the URI; this is the
        // _ID value. Then, append the value to the WHERE clause for
        // the query
        newSelection += "and _ID = " + uri.getLastPathSegment();
        break;
      case CLIP_LABEL_MAP_JOIN:
        // special case for filtering by Label
        final String table = ClipsContract.Clip.TABLE_NAME + " INNER JOIN " +
          ClipsContract.LabelMap.TABLE_NAME + " ON " +
          ClipsContract.Clip.TABLE_NAME + '.' +
          ClipsContract.Clip._ID + " = " +
          ClipsContract.LabelMap.TABLE_NAME + '.' +
          ClipsContract.LabelMap.COL_CLIP_ID;
        queryBuilder.setTables(table);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipsContract.Clip.getDefaultSortOrder();
        }
        // set to Clip Uri for notifications
        newUri = ClipsContract.Clip.CONTENT_URI;
         break;
      default:
        throw new IllegalArgumentException(UNKNOWN_URI + uri);
    }

    // Do the query
    final SQLiteDatabase db = App.getDbHelper().getReadableDatabase();
    final Cursor cursor = queryBuilder.query(
      db,
      projection,
      newSelection,
      selectionArgs,
      null,
      null,
      newSortOrder);

    // set notifier
    final ContentResolver resolver = mContext.getContentResolver();
    cursor.setNotificationUri(resolver, newUri);

    return cursor;
  }

  @Nullable
  @Override
  public String getType(@NonNull Uri uri) {
    throw new UnsupportedOperationException("Unimplemented method: " + TAG);
  }

  @Override
  public Uri insert(@NonNull Uri uri, ContentValues values) {

    Uri newUri = uri;
    final String table;

    final int uriType = URI_MATCHER.match(uri);
    switch (uriType) {
      case CLIP:
        table = ClipsContract.Clip.TABLE_NAME;
        break;
      case LABEL:
        table = ClipsContract.Label.TABLE_NAME;
        break;
      case LABEL_MAP:
        table = ClipsContract.LabelMap.TABLE_NAME;
        break;
      case CLIP_ID:
      case LABEL_ID:
      case LABEL_MAP_ID:
        throw new IllegalArgumentException(UNSUPPORTED_URI + uri);
      default:
        throw new IllegalArgumentException(UNKNOWN_URI + uri);
    }

    // this will insert or update as needed.
    // If it updates, it will be a new PRIMARY_KEY
    final SQLiteDatabase db = App.getDbHelper().getWritableDatabase();
    final long row = db.replace(table, null, values);

    if (row != -1) {
      newUri = ContentUris.withAppendedId(uri, row);

      Log.logD(TAG, "Added or updated row from insert: " + row + " in " +
        "table: " + table);

      final ContentResolver resolver = mContext.getContentResolver();
      resolver.notifyChange(newUri, null);
    }

    return newUri;
  }

  @Override
  public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
    int insertCount = 0;
    final int uriType = URI_MATCHER.match(uri);
    final SQLiteDatabase db = App.getDbHelper().getWritableDatabase();
    final String table;

    switch (uriType) {
      case CLIP:
        table = ClipsContract.Clip.TABLE_NAME;
        break;
      case LABEL:
        table = ClipsContract.Label.TABLE_NAME;
        break;
      case LABEL_MAP:
        table = ClipsContract.LabelMap.TABLE_NAME;
        break;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_URI + uri);
    }

    db.beginTransaction();
    for (final ContentValues value : values) {
      final long id = db.insert(table, null,
        value);
      if (id > 0) {
        insertCount++;
      }
    }
    db.setTransactionSuccessful();
    db.endTransaction();

    Log.logD(TAG, "Bulk insert rows: " + insertCount + " into table: " + table);

    final ContentResolver resolver = mContext.getContentResolver();
    resolver.notifyChange(uri, null);

    return insertCount;
  }

  @Override
  public int delete(@NonNull Uri uri, String selection, String[]
    selectionArgs) {

    final int uriType = URI_MATCHER.match(uri);
    final String table;
    String newSelection = selection;

    String id;
    switch (uriType) {
      case CLIP:
        table = ClipsContract.Clip.TABLE_NAME;
        break;
      case CLIP_ID:
        table = ClipsContract.Clip.TABLE_NAME;
        id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipsContract.Clip._ID + "=" + id;
        } else {
          newSelection = selection + ClipsContract.Clip._ID + "=" + id;
        }
        break;
      case LABEL:
        table = ClipsContract.Label.TABLE_NAME;
        break;
      case LABEL_ID:
        table = ClipsContract.Label.TABLE_NAME;
        id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipsContract.Label._ID + "=" + id;
        } else {
          newSelection = selection + ClipsContract.Label._ID + "=" + id;
        }
        break;
      case LABEL_MAP:
        table = ClipsContract.LabelMap.TABLE_NAME;
        break;
      case LABEL_MAP_ID:
        table = ClipsContract.LabelMap.TABLE_NAME;
        id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipsContract.LabelMap._ID + "=" + id;
        } else {
          newSelection = selection + ClipsContract.LabelMap._ID + "=" + id;
        }
        break;
      default:
        throw new IllegalArgumentException(UNKNOWN_URI + uri);
    }

    // do the delete
    final SQLiteDatabase db = App.getDbHelper().getWritableDatabase();
    final int rowsDeleted = db.delete(
      table,
      newSelection,
      selectionArgs);

    Log.logD(TAG, "Deleted rows: " + rowsDeleted + " in table: " + table);

    final ContentResolver resolver = mContext.getContentResolver();
    resolver.notifyChange(uri, null);

    if ((uriType == LABEL_MAP) || (uriType == LABEL_MAP_ID)) {
      // also force Clip table change in case deleted label is in
      // current MainActivity view
      resolver.notifyChange(ClipsContract.Clip.CONTENT_URI, null);
    }

    return rowsDeleted;
  }

  @Override
  public int update(@NonNull Uri uri, ContentValues values, String selection,
                    String[] selectionArgs) {
    final String table;
    String newSelection = selection;

    String id;
    final int uriType = URI_MATCHER.match(uri);
    switch (uriType) {
      case CLIP:
        table = ClipsContract.Clip.TABLE_NAME;
        break;
      case CLIP_ID:
        table = ClipsContract.Clip.TABLE_NAME;
        id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipsContract.Clip._ID + "=" + id;
        } else {
          newSelection = newSelection + " and " +
            ClipsContract.Clip._ID + "=" + id;
        }
        break;
      case LABEL:
        table = ClipsContract.Label.TABLE_NAME;
        break;
      case LABEL_ID:
        table = ClipsContract.Label.TABLE_NAME;
        id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipsContract.Label._ID + "=" + id;
        } else {
          newSelection = newSelection + " and " +
            ClipsContract.Label._ID + "=" + id;
        }
        break;
      case LABEL_MAP:
        table = ClipsContract.LabelMap.TABLE_NAME;
        break;
      case LABEL_MAP_ID:
        table = ClipsContract.LabelMap.TABLE_NAME;
        id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipsContract.LabelMap._ID + "=" + id;
        } else {
          newSelection = newSelection + " and " +
            ClipsContract.LabelMap._ID + "=" + id;
        }
        break;
      default:
        throw new IllegalArgumentException(UNKNOWN_URI + uri);
    }

    // do the update
    final SQLiteDatabase db = App.getDbHelper().getWritableDatabase();
    final int rowsUpdated = db.update(
      table,
      values,
      newSelection,
      selectionArgs);

    final ContentResolver resolver = mContext.getContentResolver();
    resolver.notifyChange(uri, null);

    Log.logD(TAG, "Updated rows: " + rowsUpdated + " in table: " + table);

    return rowsUpdated;
  }
}
