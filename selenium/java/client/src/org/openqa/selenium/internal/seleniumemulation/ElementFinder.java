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

package org.openqa.selenium.internal.seleniumemulation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import com.thoughtworks.selenium.SeleniumException;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class ElementFinder {

  private final String findElement;
  private Map<String, String> additionalLocators = Maps.newHashMap();

  @VisibleForTesting
  protected ElementFinder() {
    findElement = null;
  }

  public ElementFinder(JavascriptLibrary library) {
    String rawScript = library.getSeleniumScript("findElement.js");
    findElement = "return (" + rawScript + ")(arguments[0]);";

    String linkTextLocator = "return (" + library.getSeleniumScript("linkLocator.js") + ").call(null, arguments[0], document)";

    add("link", linkTextLocator);
  }

  public WebElement findElement(WebDriver driver, String locator) {
    WebElement toReturn = null;

    String strategy = searchAdditionalStrategies(locator);
    if (strategy != null) {
      String actualLocator = locator.substring(locator.indexOf('=') + 1);
      // TODO(simon): Recurse into child documents

      try {
        toReturn = (WebElement) ((JavascriptExecutor) driver).executeScript(strategy, actualLocator);

        if (toReturn == null) {
          throw new SeleniumException("Element " + locator + " not found");
        }

        return toReturn;
      } catch (WebDriverException e) {
        throw new SeleniumException("Element " + locator + " not found");
      }
    }

    try {
      toReturn = findElementDirectlyIfNecessary(driver, locator);
      if (toReturn != null) {
        return toReturn;
      }

      return (WebElement) ((JavascriptExecutor) driver)
          .executeScript(findElement, locator);
    } catch (WebDriverException e) {
      throw new SeleniumException("Element " + locator + " not found", e);
    }
  }

  public void add(String strategyName, String implementation) {
    additionalLocators.put(strategyName, implementation);
  }

  private String searchAdditionalStrategies(String locator) {
    int index = locator.indexOf('=');
    if (index == -1) {
      return null;
    }

    String key = locator.substring(0, index);
    return additionalLocators.get(key);
  }

  private WebElement findElementDirectlyIfNecessary(WebDriver driver, String locator) {
    if (locator.startsWith("xpath=")) {
      return driver.findElement(By.xpath(locator.substring("xpath=".length())));
    }
    if (locator.startsWith("//")) {
      return driver.findElement(By.xpath(locator));
    }

    if (locator.startsWith("css=")) {
      return driver.findElement(By.cssSelector(locator.substring("css=".length())));
    }

    return null;
  }

}
