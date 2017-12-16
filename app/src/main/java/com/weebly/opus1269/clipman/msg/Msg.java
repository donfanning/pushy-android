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

package com.weebly.opus1269.clipman.msg;

/** Static constants for messaging */
public class Msg {

  // message types

  public static final String ACTION = "act";
  public static final String ACTION_MESSAGE = "m";
  public static final String ACTION_PING = "ping_others";
  public static final String ACTION_PING_RESPONSE = "respond_to_ping";
  public static final String ACTION_DEVICE_ADDED = "add_our_device";
  public static final String ACTION_DEVICE_REMOVED = "remove_our_device";

  // message data keys

  public static final String MESSAGE = "m";
  public static final String DEVICE_MODEL = "dM";
  public static final String DEVICE_SN = "dSN";
  public static final String DEVICE_OS = "dOS";
  public static final String DEVICE_NICKNAME = "dN";
  public static final String FAV = "fav";
  public static final String SRC_REG_ID = "srcRegId";

  // shared error messages

  /** Start of no devices server error */
  public static final String SERVER_ERR_NO_DEVICES =
    "No other devices are registered";

  /** Start of no db entry server error */
  static final String SERVER_ERR_NO_DB_ENTRY =
    "Your email is no longer registered with the server";


  private Msg() {
    // prevent creation
  }
}
