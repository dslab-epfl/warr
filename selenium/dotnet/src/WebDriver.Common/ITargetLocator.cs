/* Copyright notice and license
Copyright 2007-2010 WebDriver committers
Copyright 2007-2010 Google Inc.
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

namespace OpenQA.Selenium
{
    /// <summary>
    /// Defines the interface through which the user can locate a given frame or window.
    /// </summary>
    public interface ITargetLocator
    {
        /// <summary>
        /// Select a frame by its (zero-based) index.
        /// </summary>
        /// <param name="frameIndex">The zero-based index of the frame to select.</param>
        /// <returns>An <see cref="IWebDriver"/> instance focused on the specified frame.</returns>
        /// <exception cref="NoSuchFrameException">If the frame cannot be found.</exception>
        IWebDriver Frame(int frameIndex);

        /// <summary>
        /// Select a frame by its name or ID.
        /// </summary>
        /// <param name="frameName">The name of the frame to select.</param>
        /// <returns>An <see cref="IWebDriver"/> instance focused on the specified frame.</returns>
        /// <exception cref="NoSuchFrameException">If the frame cannot be found.</exception>
        IWebDriver Frame(string frameName);

        /// <summary>
        /// Select a frame using its previously located <see cref="IWebElement"/>
        /// </summary>
        /// <param name="frameElement">The frame element to switch to.</param>
        /// <returns>An <see cref="IWebDriver"/> instance focused on the specified frame.</returns>
        /// <exception cref="NoSuchFrameException">If the element is neither a FRAME nor an IFRAME element.</exception>
        /// <exception cref="StaleElementReferenceException">If the element is no longer valid.</exception>
        IWebDriver Frame(IWebElement frameElement);

        /// <summary>
        /// Switches the focus of future commands for this driver to the window with the given name.
        /// </summary>
        /// <param name="windowName">The name of the window to select.</param>
        /// <returns>An <see cref="IWebDriver"/> instance focused on the given window.</returns>
        /// <exception cref="NoSuchWindowException">If the window cannot be found.</exception>
        IWebDriver Window(string windowName);

        /// <summary>
        /// Selects either the first frame on the page or the main document when a page contains iframes.
        /// </summary>
        /// <returns>An <see cref="IWebDriver"/> instance focused on the default frame.</returns>
        IWebDriver DefaultContent();

        /// <summary>
        /// Switches to the element that currently has the focus, or the body element 
        /// if no element with focus can be detected.
        /// </summary>
        /// <returns>An <see cref="IWebElement"/> instance representing the element 
        /// with the focus, or the body element if no element with focus can be detected.</returns>
        IWebElement ActiveElement();

        /// <summary>
        /// Switches to the currently active modal dialog for this particular driver instance.
        /// </summary>
        /// <returns>A handle to the dialog.</returns>
        IAlert Alert();
    }
}
