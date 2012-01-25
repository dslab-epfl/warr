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

package org.openqa.selenium.interactions;

import java.util.List;

import org.openqa.selenium.AbstractDriverTestCase;
import org.openqa.selenium.By;
import org.openqa.selenium.HasInputDevices;
import org.openqa.selenium.Ignore;
import org.openqa.selenium.JavascriptEnabled;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import static org.openqa.selenium.Ignore.Driver.HTMLUNIT;
import static org.openqa.selenium.Ignore.Driver.ANDROID;
import static org.openqa.selenium.Ignore.Driver.IE;
import static org.openqa.selenium.Ignore.Driver.FIREFOX;
import static org.openqa.selenium.Ignore.Driver.REMOTE;
import static org.openqa.selenium.Ignore.Driver.IPHONE;
import static org.openqa.selenium.Ignore.Driver.CHROME;
import static org.openqa.selenium.Ignore.Driver.SELENESE;

/**
 * Tests combined input actions.
 */
public class CombinedInputActionsTest extends AbstractDriverTestCase {

  //TODO: Check if this could work in any browser without native events. 
  @JavascriptEnabled
  @Ignore({HTMLUNIT, ANDROID, IE, FIREFOX, REMOTE, IPHONE, CHROME, SELENESE})
  public void testClickingOnFormElements() {
    driver.get(pages.formSelectionPage);

    List<WebElement> options = driver.findElements(By.tagName("option"));

    ActionChainsGenerator actionsBuilder = ((HasInputDevices) driver).actionsBuilder();
    Action selectThreeOptions = actionsBuilder.click(options.get(1))
        .keyDown(Keys.SHIFT)
        .click(options.get(2))
        .click(options.get(3))
        .keyUp(Keys.SHIFT)
        .build();

    selectThreeOptions.perform();

    WebElement showButton = driver.findElement(By.name("showselected"));
    showButton.click();

    WebElement resultElement = driver.findElement(By.id("result"));
    assertEquals("Should have picked the last three options.", "roquefort parmigiano cheddar",
        resultElement.getText());
  }

  @JavascriptEnabled
  @Ignore({ANDROID, IE, FIREFOX, REMOTE, IPHONE, CHROME, SELENESE})
  public void testSelectingMultipleItems() {
    driver.get(pages.selectableItemsPage);

    WebElement reportingElement = driver.findElement(By.id("infodiv"));

    assertEquals("no info", reportingElement.getText());

    List<WebElement> listItems = driver.findElements(By.tagName("li"));

    ActionChainsGenerator actionsBuilder = ((HasInputDevices) driver).actionsBuilder();
    Action selectThreeItems = actionsBuilder.keyDown(Keys.CONTROL)
        .click(listItems.get(1))
        .click(listItems.get(3))
        .click(listItems.get(5))
        .keyUp(Keys.CONTROL)
        .build();

    selectThreeItems.perform();

    assertEquals("#item2 #item4 #item6", reportingElement.getText());

    // Now click on another element, make sure that's the only one selected.
    actionsBuilder = ((HasInputDevices) driver).actionsBuilder();
    actionsBuilder.click(listItems.get(6)).build().perform();
    assertEquals("#item7", reportingElement.getText());
  }
}
