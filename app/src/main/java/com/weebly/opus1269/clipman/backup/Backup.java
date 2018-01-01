/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.backup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.db.ClipTable;
import com.weebly.opus1269.clipman.db.LabelTables;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Label;
import com.weebly.opus1269.clipman.ui.backup.BackupActivity;

import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/** Singleton to manage Google DriveHelper data backups */
public class Backup {
  // OK, because mContext is the global Application context
  @SuppressLint("StaticFieldLeak")
  private static Backup sInstance;

  /** Global Application Context */
  private final Context mContext;

  private final String TAG = this.getClass().getSimpleName();

  /** filename in zip file */
  private final String BACKUP_FILNAME = "backup.txt";


  private Backup(@NonNull Context context) {
    mContext = context.getApplicationContext();
  }

  /**
   * Lazily create our instance
   * @param context any old context
   */
  public static Backup INST(@NonNull Context context) {
    synchronized (Backup.class) {
      if (sInstance == null) {
        sInstance = new Backup(context);
      }
      return sInstance;
    }
  }

  public void doBackup(BackupActivity activity) {
    DriveHelper.INST(mContext).createBackupFile(activity, getZipFilename(), getBytes());
  }

  private String getStringData() {
    // TODO need _id too
    String ret;
    ClipItem[] clipItems = ClipTable.INST(mContext).getAll(true, null);
    List<Label> labels = LabelTables.INST(mContext).getLabels();
    // get stringified JSON
    final Gson gson = new Gson();
    ret = "{\"labels\":";
    ret += gson.toJson(labels);
    ret += ",\"clipItems\":";
    ret += gson.toJson(clipItems);
    ret += "}";
    Log.logD(TAG, ret);
    return ret;
  }

  private byte[] getBytes() {
    ZipEntrySource[] entries = new ZipEntrySource[]{
      new ByteSource(BACKUP_FILNAME, getStringData().getBytes())
    };
    BufferedOutputStream out = null;
    BufferedInputStream in = null;
    ByteArrayOutputStream data = null;
    try {
      data = new ByteArrayOutputStream();
      out = new BufferedOutputStream(data);
      in = new BufferedInputStream(new ByteArrayInputStream(new byte[]{}));
      ZipUtil.addEntries(in, entries, out);
      out.flush();
      //byte[] output = data.toByteArray();
      //for (int i = 0; i < output.length; i++) {
      //  Log.logD(TAG, "" + output[i]);
      //}
    } catch (Exception ex) {
      Log.logEx(mContext, TAG, ex.getLocalizedMessage(), ex, true);
    } finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
    }
    return data.toByteArray();
  }

  private String getZipFilename() {
    String ret = Device.getMyOS() + Device.getMySN(mContext) + ".zip";
    ret = ret.replace(' ', '_');
    return ret;
  }
}
