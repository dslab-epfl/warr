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
package org.openqa.selenium;

import junit.framework.TestCase;

/**
 * Small test for name extraction
 * @author eran.mes@gmail.com
 */
public class WebDriverExceptionTest extends TestCase {
  public void testExtractsADriverName() {
    StackTraceElement[] stackTrace = new StackTraceElement[2];
    stackTrace[0] = new StackTraceElement("SomeClass", "someMethod", "SomeClass.java", 5);
    stackTrace[1] = new StackTraceElement("TestDriver", "someMethod", "TestDriver.java", 5);

    String gotName = WebDriverException.getDriverName(stackTrace);

    assertEquals("TestDriver", gotName);
  }

  public void testExtractsMostSpecificDriverName() {
    StackTraceElement[] stackTrace = new StackTraceElement[3];
    stackTrace[0] = new StackTraceElement("SomeClass", "someMethod", "SomeClass.java", 5);
    stackTrace[1] = new StackTraceElement("RemoteWebDriver", "someMethod", "RemoteWebDriver.java", 5);
    stackTrace[2] = new StackTraceElement("FirefoxDriver", "someMethod", "FirefoxDriver.java", 5);

    String gotName = WebDriverException.getDriverName(stackTrace);

    assertEquals("FirefoxDriver", gotName);

  }

  public void testDefaultsToUnknownDriverName() {
    StackTraceElement[] stackTrace = new StackTraceElement[2];
    stackTrace[0] = new StackTraceElement("SomeClass", "someMethod", "SomeClass.java", 5);
    stackTrace[1] = new StackTraceElement("SomeOtherClass", "someMethod", "SomeOtherClass.java", 5);

    String gotName = WebDriverException.getDriverName(stackTrace);

    assertEquals("unknown", gotName);
  }

}
