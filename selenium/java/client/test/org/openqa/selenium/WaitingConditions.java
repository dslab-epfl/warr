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

import java.util.concurrent.Callable;

public class WaitingConditions {

  private WaitingConditions() {
    // utility class
  }

  public static Callable<WebElement> elementToExist(
      final WebDriver driver, final String elementId) {
    return new Callable<WebElement>() {

      public WebElement call() throws Exception {
        return driver.findElement(By.id(elementId));
      }

      @Override
      public String toString() {
        return String.format("element with ID %s to exist", elementId);
      }
    };
  }

  public static Callable<String> elementTextToEqual(
      final WebElement element, final String value) {
    return new Callable<String>() {

      public String call() throws Exception {
        String text = element.getText();
        if (value.equals(text)) {
          return text;
        }

        return null;
      }

      @Override
      public String toString() {
        return "element text did not equal: " + value;
      }
    };
  }

  public static Callable<String> elementTextToEqual(
     final WebDriver driver, final By locator, final String value) {
    return new Callable<String>() {

      public String call() throws Exception {
        String text = driver.findElement(locator).getText();
        if (value.equals(text)) {
          return text;
        }

        return null;
      }

      @Override
      public String toString() {
        return "element text did not equal: " + value;
      }
    };
  }



  public static Callable<String> elementTextToContain(
      final WebElement element, final String value) {
    return new Callable<String>() {

      public String call() throws Exception {
        String text = element.getText();
        if (text.contains(value)) {
          return text;
        }

        return null;
      }

      @Override
      public String toString() {
        return "element text to contain: " + value;
      }
    };
  }

  public static Callable<String> elementValueToEqual(
      final WebElement element, final String expectedValue) {
    return new Callable<String>() {

      public String call() throws Exception {
        String value = element.getValue();
        if (expectedValue.equals(value)) {
          return value;
        }

        return null;
      }

      @Override
      public String toString() {
        return "element value to equal: " + expectedValue;
      }
    };
  }

  public static Callable<String> pageTitleToBe(
      final WebDriver driver, final String expectedTitle) {
    return new Callable<String>() {

      public String call() throws Exception {
        String title = driver.getTitle();

        if (expectedTitle.equals(title)) {
          return title;
        }

        return null;
      }

      @Override
      public String toString() {
        return "title to be: " + expectedTitle;
      }
    };
  }
}
