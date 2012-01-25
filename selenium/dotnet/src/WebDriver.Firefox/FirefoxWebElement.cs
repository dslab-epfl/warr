﻿﻿/* Copyright notice and license
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
using System.Collections.ObjectModel;
using System.Drawing;
using System.Globalization;
using System.Text;
using OpenQA.Selenium.Internal;
using OpenQA.Selenium.Remote;

namespace OpenQA.Selenium.Firefox
{
    /// <summary>
    /// Allows the user to control elements on a page in Firefox.
    /// </summary>
    public class FirefoxWebElement : RenderedRemoteWebElement
    {
        #region Constructor
        /// <summary>
        /// Initializes a new instance of the <see cref="FirefoxWebElement"/> class.
        /// </summary>
        /// <param name="parentDriver">The <see cref="FirefoxDriver"/> instance hosting this element.</param>
        /// <param name="id">The ID assigned to the element.</param>
        public FirefoxWebElement(FirefoxDriver parentDriver, string id)
            : base(parentDriver, id)
        {
        }
        #endregion

        #region Overrides
        /// <summary>
        /// Determines whether two <see cref="FirefoxWebElement"/> instances are equal.
        /// </summary>
        /// <param name="obj">The <see cref="FirefoxWebElement"/> to compare with the current <see cref="FirefoxWebElement"/>.</param>
        /// <returns><see langword="true"/> if the specified <see cref="FirefoxWebElement"/> is equal to the 
        /// current <see cref="FirefoxWebElement"/>; otherwise, <see langword="false"/>.</returns>
        public override bool Equals(object obj)
        {
            IWebElement other = obj as IWebElement;

            if (other == null)
            {
                return false;
            }

            if (other is IWrapsElement)
            {
                other = ((IWrapsElement)obj).WrappedElement;
            }

            FirefoxWebElement otherAsElement = other as FirefoxWebElement;
            if (otherAsElement == null)
            {
                return false;
            }

            return Id == otherAsElement.Id;
        }

        /// <summary>
        /// Serves as a hash function for a <see cref="FirefoxWebElement"/>.
        /// </summary>
        /// <returns>A hash code for the current <see cref="FirefoxWebElement"/>.</returns>
        public override int GetHashCode()
        {
            return base.GetHashCode();
        }
        #endregion
    }
}
