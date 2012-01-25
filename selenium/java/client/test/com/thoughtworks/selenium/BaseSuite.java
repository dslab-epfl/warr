/*
Copyright 2011 WebDriver committers
Copyright 2011 Google Inc.

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

package com.thoughtworks.selenium;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openqa.selenium.environment.GlobalTestEnvironment;
import org.openqa.selenium.environment.TestEnvironment;
import org.openqa.selenium.v1.SeleniumTestEnvironment;

public class BaseSuite {
  @BeforeClass
  public static void initializeServer() {
    GlobalTestEnvironment.get(SeleniumTestEnvironment.class);
  }

  @AfterClass
  public static void shutdown() {
    TestEnvironment environment = GlobalTestEnvironment.get();
    if (environment != null) {
      environment.stop();
      GlobalTestEnvironment.set(null);
    }
  }
}
