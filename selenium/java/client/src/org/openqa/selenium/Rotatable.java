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

/**
 * Represents rotation of the browser view for orientation-sensitive devices.
 * 
 * When using this with a real device, the device should not be moved so that the
 * built-in sensors do not interfere.
 */
public interface Rotatable {

  /**
   * Changes the orientation of the browser window.
   * 
   * @param orientation the desired screen orientation
   */
  void rotate(ScreenOrientation orientation);
  
  /**
   * @return the current screen orientation of the browser
   */
  ScreenOrientation getOrientation();
}
