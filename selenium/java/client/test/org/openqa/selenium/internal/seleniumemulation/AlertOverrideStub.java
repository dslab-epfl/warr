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

package org.openqa.selenium.internal.seleniumemulation;

import org.openqa.selenium.WebDriver;

public class AlertOverrideStub extends AlertOverride  {
  @Override
  public void replaceAlertMethod(WebDriver driver) {}

  @Override
  public String getNextAlert(WebDriver driver) {
    return null;
  }

  @Override
  public boolean isAlertPresent(WebDriver driver) {
    return false;
  }

  @Override
  public String getNextConfirmation(WebDriver driver) {
    return null;
  }

  @Override
  public boolean isConfirmationPresent(WebDriver driver) {
    return false;
  }
}
