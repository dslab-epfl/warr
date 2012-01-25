using System;
using OpenQA.Selenium;

namespace Selenium.Internal.SeleniumEmulation
{
    /// <summary>
    /// Defines the command for the type keyword.
    /// </summary>
    internal class Type : SeleneseCommand
    {
        private AlertOverride alertOverride;
        private ElementFinder finder;
        private KeyState state;

        /// <summary>
        /// Initializes a new instance of the <see cref="Type"/> class.
        /// </summary>
        /// <param name="alertOverride">An <see cref="AlertOverride"/> object used to handle JavaScript alerts.</param>
        /// <param name="elementFinder">An <see cref="ElementFinder"/> used to find the element on which to execute the command.</param>
        /// <param name="keyState">A <see cref="KeyState"/> object tracking the state of modifier keys.</param>
        public Type(AlertOverride alertOverride, ElementFinder elementFinder, KeyState keyState)
        {
            this.alertOverride = alertOverride;
            this.finder = elementFinder;
            this.state = keyState;
        }

        /// <summary>
        /// Handles the command.
        /// </summary>
        /// <param name="driver">The driver used to execute the command.</param>
        /// <param name="locator">The first parameter to the command.</param>
        /// <param name="value">The second parameter to the command.</param>
        /// <returns>The result of the command.</returns>
        protected override object HandleSeleneseCommand(IWebDriver driver, string locator, string value)
        {
            this.alertOverride.ReplaceAlertMethod();
            if (this.state.ControlKeyDown || this.state.AltKeyDown || this.state.MetaKeyDown)
            {
                throw new SeleniumException("type not supported immediately after call to controlKeyDown() or altKeyDown() or metaKeyDown()");
            }

            string stringToType = this.state.ShiftKeyDown ? value.ToUpperInvariant() : value;

            IWebElement element = this.finder.FindElement(driver, locator);
            IJavaScriptExecutor executor = driver as IJavaScriptExecutor;
            if (executor != null && executor.IsJavaScriptEnabled)
            {
                JavaScriptLibrary.CallEmbeddedSelenium(driver, "replaceText", element, stringToType);
            }
            else
            {
                element.SendKeys(stringToType);
            }

            return null;
        }
    }
}
