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

package org.openqa.selenium.android.events;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.webkit.WebView;

import com.google.common.collect.Lists;

import org.openqa.selenium.android.Platform;

import java.util.List;

/**
 * Class used to send touch events to the screen directed to the webview.
 */
public class TouchScreen {  
  public static void sendMotion(WebView webview, MotionEvent... events) {
    WebViewAction.clearFocusFromCurrentElement(webview);
    
    if (Platform.sdk() <= Platform.DONUT) {
      webview.pauseTimers();
    }
    try {
      List<MotionEvent> eventsQueue = Lists.newLinkedList();      
      long downTime = SystemClock.uptimeMillis();
      for (MotionEvent event : events) {
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent e = MotionEvent.obtain(downTime, eventTime, event.getAction(), event.getX(),
            event.getY(), event.getMetaState());
        eventsQueue.add(e);
      }
      for (MotionEvent me : eventsQueue) {
        webview.onTouchEvent(me);
      }
      for (MotionEvent me : eventsQueue) {
        me.recycle();
      }
    } finally {
      if (Platform.sdk() <= Platform.DONUT) {
        webview.resumeTimers();
      }
    }
  }
}
