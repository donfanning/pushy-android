/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.model;

/** Immutable Error message */
public class ErrorMsg {
  public final String title;
  public final String msg;

  public ErrorMsg() {
    this.title = "";
    this.msg = "";
  }

  public ErrorMsg(String msg) {
    this.title = "";
    this.msg = msg;
  }

  public ErrorMsg(String title, String msg) {
    this.title = title;
    this.msg = msg;
  }
}
