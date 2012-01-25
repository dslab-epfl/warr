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

package org.openqa.selenium;

import java.util.List;

import org.openqa.selenium.internal.WrapsDriver;

public class StubElement implements WebElement, WrapsDriver {
  public void click() {
  }

  public void submit() {
  }

  public String getValue() {
    return null;
  }

  public void sendKeys(CharSequence... keysToSend) {
  }

  public void clear() {
  }

  public String getTagName() {
    return null;
  }

  public String getAttribute(String name) {
    return null;
  }

  public boolean toggle() {
    return false;
  }

  public boolean isSelected() {
    return false;
  }

  public void setSelected() {
  }

  public boolean isEnabled() {
    return false;
  }

  public String getText() {
    return null;
  }

  public List<WebElement> findElements(By by) {
    return null;
  }

  public WebElement findElement(By by) {
    return null;
  }

  public WebDriver getWrappedDriver() {
    return null;
  }
}
