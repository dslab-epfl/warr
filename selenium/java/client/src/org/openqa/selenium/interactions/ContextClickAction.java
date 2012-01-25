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

import org.openqa.selenium.Mouse;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.interactions.internal.BaseAction;
import org.openqa.selenium.interactions.internal.MouseRelatedAction;

/**
 * Context-clicks an element
 *
 */
public class ContextClickAction extends MouseRelatedAction implements Action {
  public ContextClickAction(Mouse mouse, Locatable where) {
    super(mouse, where);
  }

  /**
   * Emulates clicking on the mouse button that would bring up contextual
   * menus (usually right-clicking).
   */
  public void perform() {
    moveToLocation();
    mouse.contextClick(getActionLocation());
  }

}
