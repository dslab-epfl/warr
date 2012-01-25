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

package org.openqa.selenium.android.app;


/**
 * Custom module that is added to the WebView's JavaScript engine to enable callbacks to java
 * code. This is required since WebView doesn't expose the underlying DOM.
 */
public final class JavascriptInterface {
  private final JavascriptExecutor executor;

  public JavascriptInterface(JavascriptExecutor executor) {
    this.executor = executor;
  }
  
  /**
   * A callback from JavaScript to Java that passes execution result as a parameter.
   *
   * This method is accessible from WebView's JS DOM as windows.webdriver.resultMethod().
   *
   * @param result Result that should be returned to Java code from WebView.
   */
  public void resultMethod(String result) {
    executor.resultAvailable(result);
  }
}
