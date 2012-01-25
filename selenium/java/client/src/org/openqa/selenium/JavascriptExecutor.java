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

package org.openqa.selenium;

/**
 * Indicates that a driver can execute Javascript, providing access to the mechanism to do so.
 */
public interface JavascriptExecutor {
  /**
   * Execute javascript in the context of the currently selected frame or
   * window. This means that "document" will refer to the current document.
   * If the script has a return value, then the following steps will be taken:
   *
   * <ul> <li>For an HTML element, this method returns a WebElement</li>
   * <li>For a decimal, a Double is returned</li>
   * <li>For a non-decimal number, a Long is returned</li>
   * <li>For a boolean, a Boolean is returned</li>
   * <li>For all other cases, a String is returned.</li>
   * <li>For an array, return a List&lt;Object&gt; with each object
   * following the rules above.  We support nested lists.</li>
   * <li>Unless the value is null or there is no return value,
   * in which null is returned</li> </ul>
   *
   * <p>Arguments must be a number, a boolean, a String, WebElement,
   * or a List of any combination of the above. An exception will be
   * thrown if the arguments do not meet these criteria. The arguments
   * will be made available to the javascript via the "arguments" magic
   * variable, as if the function were called via "Function.apply"
   *
   * @param script The javascript to execute
   * @param args The arguments to the script. May be empty
   * @return One of Boolean, Long, String, List or WebElement. Or null.
   */
  Object executeScript(String script, Object... args);

  /**
   * Execute an asynchronous piece of JavaScript in the context of the
   * currently selected frame or window. Unlike executing
   * {@link #executeScript(String, Object...) synchronous JavaScript}, scripts
   * executed with this method must explicitly signal they are finished by
   * invoking the provided callback. This callback is always injected into the
   * executed function as the last argument.
   *
   * <p/>The first argument passed to the callback function will be uesd as the
   * script's result. This value will be handled as follows:
   * 
   * <ul> <li>For an HTML element, this method returns a WebElement</li>
   * <li>For a number, a Long is returned</li>
   * <li>For a boolean, a Boolean is returned</li>
   * <li>For all other cases, a String is returned.</li>
   * <li>For an array, return a List&lt;Object&gt; with each object
   * following the rules above.  We support nested lists.</li>
   * <li>Unless the value is null or there is no return value,
   * in which null is returned</li> </ul>
   *
   * <p/>Example #1: Performing a sleep in the browser under test.
   * <code><pre>
   *   long start = System.currentTimeMillis();
   *   ((JavascriptExecutor) driver).executeAsyncScript(
   *       "window.setTimeout(arguments[arguments.length - 1], 500);");
   *   System.out.println(
   *       "Elapsed time: " + System.currentTimeMillis() - start);
   * </pre></code>
   *
   * <p/>Example #2: Synchronizing a test with an AJAX application:
   * <code><pre>
   *   WebElement composeButton = driver.findElement(By.id("compose-button"));
   *   composeButton.click();
   *   ((JavascriptExecutor) driver).executeAsyncScript(
   *       "var callback = arguments[arguments.length - 1];" +
   *       "mailClient.getComposeWindowWidget().onload(callback);");
   *   driver.switchTo().frame("composeWidget");
   *   driver.findElement(By.id("to")).sendKeys("bog@example.com");
   * </pre></code>
   *
   * <p/>Example #3: Injecting a XMLHttpRequest and waiting for the result:
   * <code><pre>
   *   Object response = ((JavascriptExecutor) driver).executeAsyncScript(
   *       "var callback = arguments[arguments.length - 1];" +
   *       "var xhr = new XMLHttpRequest();" +
   *       "xhr.open('GET', '/resource/data.json', true);" +
   *       "xhr.onreadystatechange = function() {" +
   *       "  if (xhr.readyState == 4) {" +
   *       "    callback(xhr.responseText);" +
   *       "  }" +
   *       "}" +
   *       "xhr.send();");
   *   JSONObject json = new JSONObject((String) response);
   *   assertEquals("cheese", json.getString("food"));
   * </pre></code>
   *
   * @param script The javascript to execute.
   * @param args The arguments to the script. May be empty.
   * @return One of Boolean, Long, String, List, WebElement, or null.
   */
  Object executeAsyncScript(String script, Object... args);

  /**
   * It's not enough to simply support javascript, it also needs to be enabled too.
   */
  boolean isJavascriptEnabled();
}
