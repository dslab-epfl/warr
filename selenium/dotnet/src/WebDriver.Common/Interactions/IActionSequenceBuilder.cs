﻿/* Copyright notice and license
Copyright 2007-2011 WebDriver committers
Copyright 2007-2011 Google Inc.
Portions copyright 2007 ThoughtWorks, Inc

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

using System;
using System.Collections.Generic;
using System.Text;

namespace OpenQA.Selenium.Interactions
{
    /// <summary>
    /// Provides methods to build a complex sequence of user interactions.
    /// </summary>
    public interface IActionSequenceBuilder
    {
        //// Keyboard-related actions.

        /// <summary>
        /// Sends a modifier key down message to the browser.
        /// </summary>
        /// <param name="theKey">The key to be sent.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        /// <remarks>The key being sent must be in the <see cref="Keys"/> enum.</remarks>
        IActionSequenceBuilder KeyDown(string theKey);

        /// <summary>
        /// Sends a modifier key down message to the specified element in the browser.
        /// </summary>
        /// <param name="element">The element to which to send the key command.</param>
        /// <param name="theKey">The key to be sent.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        /// <remarks>The key being sent must be in the <see cref="Keys"/> enum.</remarks>
        IActionSequenceBuilder KeyDown(IWebElement element, string theKey);

        /// <summary>
        /// Sends a modifier key up message to the browser.
        /// </summary>
        /// <param name="theKey">The key to be sent.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        /// <remarks>The key being sent must be in the <see cref="Keys"/> enum.</remarks>
        IActionSequenceBuilder KeyUp(string theKey);

        /// <summary>
        /// Sends a modifier up down message to the specified element in the browser.
        /// </summary>
        /// <param name="element">The element to which to send the key command.</param>
        /// <param name="theKey">The key to be sent.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        /// <remarks>The key being sent must be in the <see cref="Keys"/> enum.</remarks>
        IActionSequenceBuilder KeyUp(IWebElement element, string theKey);

        /// <summary>
        /// Sends a sequence of keystrokes to the browser.
        /// </summary>
        /// <param name="keysToSend">The keystrokes to send to the browser.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder SendKeys(string keysToSend);

        /// <summary>
        /// Sends a sequence of keystrokes to the specified element in the browser.
        /// </summary>
        /// <param name="element">The element to which to send the keystrokes.</param>
        /// <param name="keysToSend">The keystrokes to send to the browser.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder SendKeys(IWebElement element, string keysToSend);

        //// Mouse-related actions.
       
        /// <summary>
        /// Clicks and holds the mouse button down on the specified element.
        /// </summary>
        /// <param name="onElement">The element on which to click and hold.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder ClickAndHold(IWebElement onElement);

        /// <summary>
        /// Releases the mouse button on the specified element.
        /// </summary>
        /// <param name="onElement">The element on which to release the button.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder Release(IWebElement onElement);

        /// <summary>
        /// Clicks the mouse on the specified element.
        /// </summary>
        /// <param name="onElement">The element on which to click.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder Click(IWebElement onElement);

        /// <summary>
        /// Clicks the mouse at the last known mouse coordinates.
        /// </summary>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder Click();

        /// <summary>
        /// Double-clicks the mouse on the specified element.
        /// </summary>
        /// <param name="onElement">The element on which to double-click.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder DoubleClick(IWebElement onElement);

        /// <summary>
        /// Moves the mouse to the specified element.
        /// </summary>
        /// <param name="toElement">The element to which to move the mouse.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder MoveToElement(IWebElement toElement);

        /// <summary>
        /// Moves the mouse to the specified offset of the top-left corner of the specified element.
        /// </summary>
        /// <param name="toElement">The element to which to move the mouse.</param>
        /// <param name="offsetX">The horizontal offset to which to move the mouse.</param>
        /// <param name="offsetY">The vertical offset to which to move the mouse.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder MoveToElement(IWebElement toElement, int offsetX, int offsetY);

        /// <summary>
        /// Moves the mouse to the specified offset of the last known mouse coordinates.
        /// </summary>
        /// <param name="offsetX">The horizontal offset to which to move the mouse.</param>
        /// <param name="offsetY">The vertical offset to which to move the mouse.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder MoveByOffset(int offsetX, int offsetY);

        /// <summary>
        /// Right-clicks the mouse on the specified element.
        /// </summary>
        /// <param name="onElement">The element on which to right-click.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder ContextClick(IWebElement onElement);

        /// <summary>
        /// Performs a drag-and-drop operation from one element to another.
        /// </summary>
        /// <param name="source">The element on which the drag operation is started.</param>
        /// <param name="target">The element on which the drop is performed.</param>
        /// <returns>A self-reference to this <see cref="IActionSequenceBuilder"/>.</returns>
        IActionSequenceBuilder DragAndDrop(IWebElement source, IWebElement target);

        /// <summary>
        /// Builds the sequence of actions.
        /// </summary>
        /// <returns>A composite <see cref="IAction"/> which can be used to perform the actions.</returns>
        IAction Build();
    }
}
