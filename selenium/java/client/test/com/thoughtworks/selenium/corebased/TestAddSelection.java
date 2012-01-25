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

package com.thoughtworks.selenium.corebased;

import com.thoughtworks.selenium.InternalSelenseTestBase;
import org.junit.Test;

public class TestAddSelection extends InternalSelenseTestBase {
  @Test
  public void addingToSelectionWhenSelectHasEmptyMultipleAttribute() {
    selenium.open("../tests/html/test_multiple_select.html");

    selenium.addSelection("sel", "select_2");
    selenium.addSelection("sel", "select_3");

    String[] found = selenium.getSelectedIds("name=sel");

    assertEquals(2, found.length);
    assertEquals("select_2", found[0]);
    assertEquals("select_3", found[1]);
  }
}
