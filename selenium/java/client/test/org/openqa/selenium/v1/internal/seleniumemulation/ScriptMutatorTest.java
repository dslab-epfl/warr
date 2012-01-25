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


package org.openqa.selenium.v1.internal.seleniumemulation;

import com.thoughtworks.selenium.Selenium;
import org.openqa.selenium.AbstractDriverTestCase;
import org.openqa.selenium.WebDriverBackedSelenium;


public class ScriptMutatorTest extends AbstractDriverTestCase {

  public void testShouldBeAbleToUseTheBrowserbot() {
    String url = pages.tables;
    Selenium selenium = new WebDriverBackedSelenium(driver, url);
    selenium.open(pages.tables);

    String rowCount = selenium.getEval(
        "var table = selenium.browserbot.findElement('id=foo'); " +
        "table.rows[0].cells.length;");

    assertEquals("3", rowCount);
  }
}
