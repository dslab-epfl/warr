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

package org.openqa.selenium;

import org.openqa.selenium.interactions.internal.Coordinates;

/**
 * Interface representing basic Mouse operations.
 *
 */
public interface Mouse {
  void click(Coordinates where);

  void doubleClick(Coordinates where);

  void mouseDown(Coordinates where);
  void mouseUp(Coordinates where);

  void mouseMove(Coordinates where);
  /* Offset from the current location of the mouse pointer. */
  void mouseMove(Coordinates where, long xOffset, long yOffset);
  // Right-clicks an element. 
  void contextClick(Coordinates where);
  
  // TODO: Scroll wheel support
}
