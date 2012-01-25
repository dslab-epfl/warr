/*
Copyright 2010 WebDriver committers
Copyright 2010 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.openqa.selenium.android.intents;

import com.google.common.base.Splitter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;

import org.openqa.selenium.android.Logger;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class IntentSender extends BroadcastReceiver implements Callable {
  private static final String LOG_TAG = IntentSender.class.getName();
  // Android intents fails to handle extra String arguments longer than 258022.
  // The value 258022 was determined empirically.
  private static final int MAX_INTENT_STRING_ARG_LENGTH = 258022;
  private boolean received = false;
  private final Context sender;
  
  private Object toReturn;
  private String action;
  
  public static final String IS_PARCELABLE = "isParcelable";

  public IntentSender(Context sender) {
    this.sender = sender;
  }
  
  public synchronized void broadcast(String action, Object... args) {
    Logger.log(Log.DEBUG, LOG_TAG, String.format("Context: %s, Sending Intent: %s, Args: %s",
        sender.toString(), action, args.length));
    received = false;
    this.action = action;
    Intent intent = new Intent(action);
    boolean isParcelable = false;
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        if (args[i] instanceof Parcelable) {
          intent.putExtra("arg_" + i, (Parcelable) args[i]);
          isParcelable = true;
        } else {
          if (args[i] instanceof String
              && args[i].toString().length() > MAX_INTENT_STRING_ARG_LENGTH) {
            intent.putExtra("arg_" + i,
                (Serializable) Splitter.fixedLength(MAX_INTENT_STRING_ARG_LENGTH).split(
                    args[i].toString()).iterator().next());
          } else {
            intent.putExtra("arg_" + i, (Serializable) args[i]);
          }
        }
      }
      intent.putExtra(IS_PARCELABLE, isParcelable);
    }
    // We supply this as the final receiver at the end of the broadcast. The onReceive
    // method will be the last one to handle the intent.
    sender.sendOrderedBroadcast(intent, null, this, null, Activity.RESULT_OK, null, null);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    received = true;
    toReturn = getResultExtras(true).get(action);
    Logger.log(Log.DEBUG, LOG_TAG,
        String.format("Received intent: %s, from context: %s, with data: %s. ",
            intent.getAction(), context, (toReturn == null ? "null" : toReturn.toString())));
  }

  public Object call() throws Exception {
    while(!received) {
      continue;
    }
    return toReturn;
  }
}
