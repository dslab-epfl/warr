/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.

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

package org.openqa.selenium.remote.server.handler.interactions;

import org.openqa.selenium.HasInputDevices;
import org.openqa.selenium.Mouse;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.remote.server.DriverSessions;
import org.openqa.selenium.remote.server.KnownElements;
import org.openqa.selenium.remote.server.handler.WebDriverHandler;
import org.openqa.selenium.remote.server.handler.WebElementHandler;
import org.openqa.selenium.remote.server.rest.ResultType;

public class MouseUp extends WebDriverHandler {

  public MouseUp(DriverSessions sessions) {
    super(sessions);
  }

  public ResultType call() throws Exception {
    Mouse mouse = ((HasInputDevices) getDriver()).getMouse();
    mouse.mouseUp(null);
    return ResultType.SUCCESS;
  }
  
  @Override
  public String toString() {
    return String.format("[mouseup: %s]", "nothing");
  }
}
