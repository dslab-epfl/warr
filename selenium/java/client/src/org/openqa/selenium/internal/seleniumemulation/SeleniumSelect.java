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

import com.google.common.collect.Lists;

import com.thoughtworks.selenium.SeleniumException;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class SeleniumSelect {

  private final String findOption;
  private final WebDriver driver;
  private final WebElement select;

  public SeleniumSelect(JavascriptLibrary library, ElementFinder finder, WebDriver driver, String locator) {
    this.driver = driver;

    findOption = "return (" + library.getSeleniumScript("findOption.js") + ").apply(null, arguments)";

    select = finder.findElement(driver, locator);
    if (!"select".equals(select.getTagName().toLowerCase())) {
      throw new SeleniumException("Element is not a select element: " + locator);
    }
  }

  public void setSelected(String optionLocator) {
    if (isMultiple()) {
      for (WebElement opt : select.findElements(By.tagName("option"))) {
        if (opt.isSelected()) {
          opt.toggle();
        }
      }
    }
    
    WebElement option = findOption(optionLocator);
    option.setSelected();
  }

  public void addSelection(String optionLocator) {
    assertSupportsMultipleSelections();

    WebElement option = findOption(optionLocator);
    option.setSelected();
  }

  public void removeSelection(String optionLocator) {
    assertSupportsMultipleSelections();

    WebElement option = findOption(optionLocator);
    if (option.isSelected()) {
      option.toggle();
    }
  }

  public List<WebElement> getSelectedOptions() {
    List<WebElement> toReturn = Lists.newArrayList();

    for (WebElement option : select.findElements(By.tagName("option"))) {
      if (option.isSelected()) {
        toReturn.add(option);
      }
    }

    return toReturn;
  }

  private WebElement findOption(String optionLocator) {
    return (WebElement) ((JavascriptExecutor) driver)
        .executeScript(findOption, select, optionLocator);
  }

  private void assertSupportsMultipleSelections() {
    if (!isMultiple()) {
      throw new SeleniumException(
          "You may only add a selection to a select that supports multiple selections");
    }
  }

  private boolean isMultiple() {
    String multipleValue = select.getAttribute("multiple");
    boolean multiple = "true".equals(multipleValue) || "multiple".equals(multipleValue);
    return multiple;
  }

  public List<WebElement> getAllOptions() {
    return select.findElements(By.tagName("option"));
  }
}