/*
Copyright 2007-2010 WebDriver committers
Copyright 2007-2010 Google Inc.

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

package org.openqa.selenium.ie;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openqa.selenium.EmptyTest;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TestSuiteBuilder;

import static org.openqa.selenium.Ignore.Driver.IE;
import static org.openqa.selenium.Platform.WINDOWS;

public class InternetExplorerDriverTestSuite extends TestCase {
  public static Test suite() throws Exception {
    System.setProperty("webdriver.development", "true");

    if (Platform.getCurrent().is(WINDOWS)) {
      return new TestSuiteBuilder()
          .addSourceDir("java/client/test")
          .usingDriver(InternetExplorerDriver.class)
          .exclude(IE)
          .includeJavascriptTests()
          .keepDriverInstance()
          .outputTestNames()
          .create();
    }

    TestSuite toReturn = new TestSuite();
    toReturn.addTestSuite(EmptyTest.class);
    return toReturn;
  }
}
