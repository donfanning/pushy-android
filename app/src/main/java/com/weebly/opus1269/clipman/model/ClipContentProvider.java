/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

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

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.Log;

import org.joda.time.DateTime;

/**
 * App private {@link ContentProvider} for the saved {@link ClipItem}
 */
@SuppressWarnings("ConstantConditions")
public class ClipContentProvider extends ContentProvider {
  private static final String TAG = "ClipContentProvider";

  private static final String UNKNOWN_URI = "Unknown URI: ";
  private static final String UNSUPPORTED_URI = "Unsupported URI: ";

  // used for the UriMatcher
  private static final int CLIP = 10;
  private static final int CLIP_ID = 20;
  private static final int LABEL = 30;
  private static final int LABEL_ID = 40;
  private static final int LABEL_MAP = 50;
  private static final int LABEL_MAP_ID = 60;
  private static final UriMatcher URI_MATCHER =
    new UriMatcher(UriMatcher.NO_MATCH);

  static {
    URI_MATCHER.addURI(ClipContract.AUTHORITY, "clip", CLIP);
    URI_MATCHER.addURI(ClipContract.AUTHORITY, "clip/#", CLIP_ID);
    URI_MATCHER.addURI(ClipContract.AUTHORITY, "label", LABEL);
    URI_MATCHER.addURI(ClipContract.AUTHORITY, "label/#", LABEL_ID);
    URI_MATCHER.addURI(ClipContract.AUTHORITY, "label_map", LABEL_MAP);
    URI_MATCHER.addURI(ClipContract.AUTHORITY, "label_map/#", LABEL_MAP_ID);
  }

  /**
   * Add the contents of the ClipItem to the clipboard,
   * optionally only if item text is new
   * @param context   a {@link Context}
   * @param clipItem  the {@link ClipItem} to insert
   * @param onNewOnly only insert if item text is not in database if true
   * @return true if inserted
   */
  public static Boolean insert(Context context, ClipItem clipItem,
                               Boolean onNewOnly) {
    if ((clipItem == null) || TextUtils.isEmpty(clipItem.getText())) {
      return false;
    }

    if (onNewOnly) {
      // query for existence and skip insert if it does
      final String[] projection = {ClipContract.Clip.COL_TEXT};
      final String selection =
        "(" + ClipContract.Clip.COL_TEXT + " == ? )";
      final String[] selectionArgs = {clipItem.getText()};

      final Cursor cursor =
        context.getContentResolver()
          .query(ClipContract.Clip.CONTENT_URI, projection,
            selection, selectionArgs, null);
      if (cursor.getCount() != 0) {
        // already in database, we are done
        cursor.close();
        return false;

      }
      cursor.close();
    }

    // do it
    insert(context, clipItem);

    return true;
  }

  /**
   * Add a {@link ClipItem} to the database
   * @param context the context
   * @param item    the clip to add
   * @return the Uri of the inserted item
   */
  private static Uri insert(Context context, ClipItem item) {
    final ContentResolver resolver = context.getContentResolver();
    return resolver.insert(ClipContract.Clip.CONTENT_URI,
      item.getContentValues());
  }

  /**
   * Add a {@link Label} to the database, if new
   * @param context the context
   * @param label   the label to add
   * @return true if inserted
   */
  @NonNull
  public static Boolean insert(Context context, Label label) {
    if ((label == null) || TextUtils.isEmpty(label.getName())) {
      return false;
    }

    // query for existence and skip insert if it does
    final String[] projection = {ClipContract.Label.COL_NAME};
    final String selection =
      "(" + ClipContract.Label.COL_NAME + " == ? )";
    final String[] selectionArgs = {label.getName()};

    final Cursor cursor =
      context.getContentResolver()
        .query(ClipContract.Label.CONTENT_URI, projection,
          selection, selectionArgs, null);
    if (cursor.getCount() != 0) {
      // already in database, we are done
      cursor.close();
      return false;

    }
    cursor.close();

    // do it
    final ContentResolver resolver = context.getContentResolver();
    resolver.insert(ClipContract.Label.CONTENT_URI,
      label.getContentValues());

    return true;
  }

  /**
   * Add a group of {@link ClipItem} objects to the databse
   * @param context a context
   * @param clipItems the items to add
   * @return number of items added
   */
  public static int insertClipItems(Context context, ContentValues[] clipItems) {
    final ContentResolver resolver = context.getContentResolver();
    return resolver.bulkInsert(ClipContract.Clip.CONTENT_URI, clipItems);
  }

  /**
   * Get the non-favorite and optionally favorite rows in the database
   * @param context     our {@link Context}
   * @param includeFavs flag to indicate if favorites should be retrieved too
   * @return Array of {@link ContentValues}
   */
  public static ContentValues[] getAll(Context context, Boolean includeFavs) {
    final String[] projection = ClipContract.Clip.FULL_PROJECTION;

    // Select all non-favorites
    String selection = "(" + ClipContract.Clip.COL_FAV + " == 0 " + ")";
    if (includeFavs) {
      // select all favorites too
      selection += " OR (" + ClipContract.Clip.COL_FAV + " == 1 )";
    }

    final ContentResolver resolver = context.getContentResolver();
    final Cursor cursor = resolver.query(
      ClipContract.Clip.CONTENT_URI,
      projection,
      selection,
      null,
      null);

    final ContentValues[] array = new ContentValues[cursor.getCount()];
    int count = 0;
    while (cursor.moveToNext()) {
      //noinspection ObjectAllocationInLoop
      final ContentValues cv = new ContentValues();
      cv.put(ClipContract.Clip.COL_TEXT, cursor.getString(cursor.getColumnIndex(ClipContract.Clip.COL_TEXT)));
      cv.put(ClipContract.Clip.COL_DATE, cursor.getLong(cursor.getColumnIndex(ClipContract.Clip.COL_DATE)));
      cv.put(ClipContract.Clip.COL_FAV, cursor.getLong(cursor.getColumnIndex(ClipContract.Clip.COL_FAV)));
      cv.put(ClipContract.Clip.COL_REMOTE, cursor.getLong(cursor.getColumnIndex(ClipContract.Clip.COL_REMOTE)));
      cv.put(ClipContract.Clip.COL_DEVICE, cursor.getString(cursor.getColumnIndex(ClipContract.Clip.COL_DEVICE)));
      array[count] = cv;
      count++;
    }
    cursor.close();

    return array;
  }

  /**
   * Delete all non-favorite and optionally favorite rows
   * @param context    a context
   * @param deleteFavs flag to indicate if favorites should be deleted
   * @return Number of rows deleted
   */
  public static int deleteAll(Context context, Boolean deleteFavs) {
    // Select all non-favorites
    String selection = "(" + ClipContract.Clip.COL_FAV + " == 0 " + ")";

    if (deleteFavs) {
      // select all favorites too
      selection = selection + " OR (" + ClipContract.Clip.COL_FAV + " == 1 )";
    }

    final ContentResolver resolver = context.getContentResolver();
    return resolver.delete(ClipContract.Clip.CONTENT_URI, selection, null);
  }

  /**
   * Delete rows older than the storage duration
   * @return Number of rows deleted
   */
  public static int deleteOldItems() {
    final Context context = App.getContext();

    final String value = Prefs.getDuration();
    if (value.equals(context.getString(R.string.ar_duration_forever))) {
      return 0;
    }

    DateTime today = DateTime.now();
    today = today.withTimeAtStartOfDay();
    DateTime deleteDate = today;
    switch (value) {
      case "day":
        deleteDate = deleteDate.minusDays(1);
        break;
      case "week":
        deleteDate = deleteDate.minusWeeks(1);
        break;
      case "month":
        deleteDate = deleteDate.minusMonths(1);
        break;
      case "year":
        deleteDate = deleteDate.minusYears(1);
        break;
      default:
        return 0;
    }

    final long deleteTime = deleteDate.getMillis();

    // Select all non-favorites older than the calculated time
    final String selection =
      "(" + ClipContract.Clip.COL_FAV + " == 0 " + ")" + " AND (" +
        ClipContract.Clip.COL_DATE + " < " + deleteTime + ")";

    final ContentResolver resolver = context.getContentResolver();
    return resolver.delete(ClipContract.Clip.CONTENT_URI, selection, null);
  }

  @Override
  public boolean onCreate() {
    return true;
  }

  @Override
  public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                      String[] selectionArgs, String sortOrder) {

    String newSelection = selection;
    String newSortOrder = sortOrder;
    final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

    final int uriType = URI_MATCHER.match(uri);
    switch (uriType) {
      case CLIP:
        queryBuilder.setTables(ClipContract.Clip.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipContract.Clip.getDefaultSortOrder();
        }
        break;
      case LABEL:
        queryBuilder.setTables(ClipContract.Label.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipContract.Label.getDefaultSortOrder();
        }
        break;
      case CLIP_ID:
        queryBuilder.setTables(ClipContract.Clip.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipContract.Clip.getDefaultSortOrder();
        }
        // Because this URI was for a single row, the _ID value part is
        // present. Get the last path segment from the URI; this is the
        // _ID value. Then, append the value to the WHERE clause for
        // the query
        newSelection += "and _ID = " + uri.getLastPathSegment();
        break;
      case LABEL_ID:
        queryBuilder.setTables(ClipContract.Label.TABLE_NAME);
        if (TextUtils.isEmpty(sortOrder)) {
          newSortOrder = ClipContract.Label.getDefaultSortOrder();
        }
        // Because this URI was for a single row, the _ID value part is
        // present. Get the last path segment from the URI; this is the
        // _ID value. Then, append the value to the WHERE clause for
        // the query
        newSelection += "and _ID = " + uri.getLastPathSegment();
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
    final ContentResolver resolver = getContext().getContentResolver();
    cursor.setNotificationUri(resolver, uri);

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
        table = ClipContract.Clip.TABLE_NAME;
        break;
      case LABEL:
        table = ClipContract.Label.TABLE_NAME;
        break;
      case LABEL_MAP:
        table = ClipContract.LabelMap.TABLE_NAME;
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

      Log.logD(TAG, "Added row from insert: " + row);

      final ContentResolver resolver = getContext().getContentResolver();
      resolver.notifyChange(uri, null);
    }

    return newUri;
  }

  @Override
  public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
    int insertCount = 0;
    final int uriType = URI_MATCHER.match(uri);
    final SQLiteDatabase db = App.getDbHelper().getWritableDatabase();

    switch (uriType) {
      case CLIP:
        db.beginTransaction();
        for (final ContentValues value : values) {
          final long id = db.insert(ClipContract.Clip.TABLE_NAME, null, value);
          if (id > 0) {
            insertCount++;
          }
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        break;
      case CLIP_ID:
        throw new IllegalArgumentException(UNSUPPORTED_URI + uri);
      default:
        throw new IllegalArgumentException(UNKNOWN_URI + uri);
    }

    Log.logD(TAG, "Bulk insert rows: " + insertCount);

    final ContentResolver resolver = getContext().getContentResolver();
    resolver.notifyChange(uri, null);

    return insertCount;
  }

  @Override
  public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

    final int uriType = URI_MATCHER.match(uri);
    final String table;
    String newSelection = selection;

    String id;
    switch (uriType) {
      case CLIP:
        table = ClipContract.Clip.TABLE_NAME;
        break;
      case CLIP_ID:
        table = ClipContract.Clip.TABLE_NAME;
        id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipContract.Clip._ID + "=" + id;
        } else {
          newSelection = selection + ClipContract.Clip._ID + "=" + id;
        }
        break;
      case LABEL:
        table = ClipContract.Label.TABLE_NAME;
        break;
      case LABEL_ID:
        table = ClipContract.Label.TABLE_NAME;
        id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipContract.Label._ID + "=" + id;
        } else {
          newSelection = selection + ClipContract.Label._ID + "=" + id;
        }
        break;
      case LABEL_MAP:
        table = ClipContract.LabelMap.TABLE_NAME;
        break;
      case LABEL_MAP_ID:
        table = ClipContract.LabelMap.TABLE_NAME;
        id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipContract.LabelMap._ID + "=" + id;
        } else {
          newSelection = selection + ClipContract.LabelMap._ID + "=" + id;
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

    Log.logD(TAG, "Deleted rows: " + rowsDeleted);

    final ContentResolver resolver = getContext().getContentResolver();
    resolver.notifyChange(uri, null);

    return rowsDeleted;
  }

  @Override
  public int update(@NonNull Uri uri, ContentValues values, String selection,
                    String[] selectionArgs) {
    final String table;
    String newSelection = selection;

    final int uriType = URI_MATCHER.match(uri);
    switch (uriType) {
      case CLIP:
        table = ClipContract.Clip.TABLE_NAME;
        break;
      case CLIP_ID:
        table = ClipContract.Clip.TABLE_NAME;
        final String id = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
          newSelection = ClipContract.Clip._ID + "=" + id;
        } else {
          newSelection = newSelection + " and " +
            ClipContract.Clip._ID + "=" + id;
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

    final ContentResolver resolver = getContext().getContentResolver();
    resolver.notifyChange(uri, null);

    Log.logD(TAG, "Updated rows: " + rowsUpdated);

    return rowsUpdated;
  }
}
