﻿/* Copyright notice and license
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
using System.Text;
using OpenQA.Selenium.Internal;

namespace OpenQA.Selenium.Support.Events
{
    /// <summary>
    /// A wrapper around an arbitrary WebDriver instance which supports registering for 
    /// events, e.g. for logging purposes.
    /// </summary>
    public class EventFiringWebDriver : IWebDriver, IJavaScriptExecutor, ITakesScreenshot, IWrapsDriver
    {
        private IWebDriver driver;

        /// <summary>
        /// Initializes a new instance of the EventFiringWebDriver class.
        /// </summary>
        /// <param name="parentDriver">The driver to register events for.</param>
        public EventFiringWebDriver(IWebDriver parentDriver)
        {
            this.driver = parentDriver;
        }

        /// <summary>
        /// Fires before the driver begins navigation.
        /// </summary>
        public event EventHandler<WebDriverNavigationEventArgs> Navigating;

        /// <summary>
        /// Fires after the driver completes navigation
        /// </summary>
        public event EventHandler<WebDriverNavigationEventArgs> Navigated;

        /// <summary>
        /// Fires before the driver begins navigation back one entry in the browser history list.
        /// </summary>
        public event EventHandler<WebDriverNavigationEventArgs> NavigatingBack;

        /// <summary>
        /// Fires after the driver completes navigation back one entry in the browser history list.
        /// </summary>
        public event EventHandler<WebDriverNavigationEventArgs> NavigatedBack;

        /// <summary>
        /// Fires before the driver begins navigation forward one entry in the browser history list.
        /// </summary>
        public event EventHandler<WebDriverNavigationEventArgs> NavigatingForward;

        /// <summary>
        /// Fires after the driver completes navigation forward one entry in the browser history list.
        /// </summary>
        public event EventHandler<WebDriverNavigationEventArgs> NavigatedForward;

        /// <summary>
        /// Fires before the driver clicks on an element.
        /// </summary>
        public event EventHandler<WebElementEventArgs> ElementClicking;

        /// <summary>
        /// Fires after the driver has clicked on an element.
        /// </summary>
        public event EventHandler<WebElementEventArgs> ElementClicked;

        /// <summary>
        /// Fires before the driver changes the value of an element via Clear(), SendKeys() or Toggle().
        /// </summary>
        public event EventHandler<WebElementEventArgs> ElementValueChanging;

        /// <summary>
        /// Fires after the driver has changed the value of an element via Clear(), SendKeys() or Toggle().
        /// </summary>
        public event EventHandler<WebElementEventArgs> ElementValueChanged;

        /// <summary>
        /// Fires before the driver starts to find an element.
        /// </summary>
        public event EventHandler<FindElementEventArgs> FindingElement;

        /// <summary>
        /// Fires after the driver completes finding an element.
        /// </summary>
        public event EventHandler<FindElementEventArgs> FindElementCompleted;

        /// <summary>
        /// Fires before a script is executed.
        /// </summary>
        public event EventHandler<WebDriverScriptEventArgs> ScriptExecuting;

        /// <summary>
        /// Fires after a script is executed.
        /// </summary>
        public event EventHandler<WebDriverScriptEventArgs> ScriptExecuted;
        
        /// <summary>
        /// Fires when an exception is thrown.
        /// </summary>
        public event EventHandler<WebDriverExceptionEventArgs> ExceptionThrown;

        #region IWrapsDriver Members
        /// <summary>
        /// Gets the <see cref="IWebDriver"/> wrapped by this EventsFiringWebDriver instance.
        /// </summary>
        public IWebDriver WrappedDriver
        {
            get { return this.driver; }
        }

        #endregion

        #region IJavaScriptExecutor Properties
        /// <summary>
        /// Gets a value indicating whether JavaScript is enabled for this browser.
        /// </summary>
        public bool IsJavaScriptEnabled
        {
            get
            {
                bool javascriptEnabled = false;
                IJavaScriptExecutor javascriptDriver = this.driver as IJavaScriptExecutor;
                if (javascriptDriver != null)
                {
                    javascriptEnabled = javascriptDriver.IsJavaScriptEnabled;
                }

                return javascriptEnabled;
            }
        }
        #endregion

        #region IWebDriver Members
        /// <summary>
        /// Gets or sets the URL the browser is currently displaying.
        /// </summary>
        /// <remarks>
        /// Setting the <see cref="Url"/> property will load a new web page in the current browser window. 
        /// This is done using an HTTP GET operation, and the method will block until the 
        /// load is complete. This will follow redirects issued either by the server or 
        /// as a meta-redirect from within the returned HTML. Should a meta-redirect "rest"
        /// for any duration of time, it is best to wait until this timeout is over, since 
        /// should the underlying page change while your test is executing the results of 
        /// future calls against this interface will be against the freshly loaded page. 
        /// </remarks>
        /// <seealso cref="INavigation.GoToUrl(System.String)"/>
        /// <seealso cref="INavigation.GoToUrl(System.Uri)"/>
        public string Url
        {
            get
            {
                return this.driver.Url;
            }

            set
            {
                WebDriverNavigationEventArgs e = new WebDriverNavigationEventArgs(this.driver, value);
                this.OnNavigating(e);
                this.driver.Url = value;
                this.OnNavigated(e);
            }
        }

        /// <summary>
        /// Gets the title of the current browser window.
        /// </summary>
        public string Title
        {
            get { return this.driver.Title; }
        }

        /// <summary>
        /// Gets the source of the page last loaded by the browser.
        /// </summary>
        /// <remarks>
        /// If the page has been modified after loading (for example, by JavaScript) 
        /// there is no guarentee that the returned text is that of the modified page. 
        /// Please consult the documentation of the particular driver being used to 
        /// determine whether the returned text reflects the current state of the page 
        /// or the text last sent by the web server. The page source returned is a 
        /// representation of the underlying DOM: do not expect it to be formatted 
        /// or escaped in the same way as the response sent from the web server. 
        /// </remarks>
        public string PageSource
        {
            get { return this.driver.PageSource; }
        }

        /// <summary>
        /// Close the current window, quitting the browser if it is the last window currently open.
        /// </summary>
        public void Close()
        {
            this.driver.Close();
        }

        /// <summary>
        /// Quits this driver, closing every associated window.
        /// </summary>
        public void Quit()
        {
            this.driver.Quit();
        }

        /// <summary>
        /// Instructs the driver to change its settings.
        /// </summary>
        /// <returns>An <see cref="IOptions"/> object allowing the user to change
        /// the settings of the driver.</returns>
        public IOptions Manage()
        {
            return new EventFiringOptions(this);
        }

        /// <summary>
        /// Instructs the driver to navigate the browser to another location.
        /// </summary>
        /// <returns>An <see cref="INavigation"/> object allowing the user to access 
        /// the browser's history and to navigate to a given URL.</returns>
        public INavigation Navigate()
        {
            return new EventFiringNavigation(this);
        }

        /// <summary>
        /// Instructs the driver to send future commands to a different frame or window.
        /// </summary>
        /// <returns>An <see cref="ITargetLocator"/> object which can be used to select
        /// a frame or window.</returns>
        public ITargetLocator SwitchTo()
        {
            return new EventFiringTargetLocator(this);
        }

        /// <summary>
        /// Get the window handles of open browser windows.
        /// </summary>
        /// <returns>A <see cref="ReadOnlyCollection{T}"/> containing all window handles
        /// of windows belonging to this driver instance.</returns>
        /// <remarks>The set of window handles returned by this method can be used to 
        /// iterate over all open windows of this <see cref="IWebDriver"/> instance by 
        /// passing them to <c>SwitchTo().Window(string)</c></remarks>
        public ReadOnlyCollection<string> GetWindowHandles()
        {
            return this.driver.GetWindowHandles();
        }

        /// <summary>
        /// Get the current window handle.
        /// </summary>
        /// <returns>An opaque handle to this window that uniquely identifies it 
        /// within this driver instance.</returns>
        public string GetWindowHandle()
        {
            return this.driver.GetWindowHandle();
        }

        #endregion

        #region ISearchContext Members
        /// <summary>
        /// Find the first <see cref="IWebElement"/> using the given method. 
        /// </summary>
        /// <param name="by">The locating mechanism to use.</param>
        /// <returns>The first matching <see cref="IWebElement"/> on the current context.</returns>
        /// <exception cref="NoSuchElementException">If no element matches the criteria.</exception>
        public IWebElement FindElement(By by)
        {
            FindElementEventArgs e = new FindElementEventArgs(this.driver, by);
            this.OnFindingElement(e);
            IWebElement element = this.driver.FindElement(by);
            this.OnFindElementCompleted(e);
            IWebElement wrappedElement = this.WrapElement(element);
            return wrappedElement;
        }

        /// <summary>
        /// Find all <see cref="IWebElement">IWebElements</see> within the current context 
        /// using the given mechanism.
        /// </summary>
        /// <param name="by">The locating mechanism to use.</param>
        /// <returns>A <see cref="ReadOnlyCollection{T}"/> of all <see cref="IWebElement">WebElements</see>
        /// matching the current criteria, or an empty list if nothing matches.</returns>
        public ReadOnlyCollection<IWebElement> FindElements(By by)
        {
            List<IWebElement> wrappedElementList = new List<IWebElement>();
            FindElementEventArgs e = new FindElementEventArgs(this.driver, by);
            this.OnFindingElement(e);
            ReadOnlyCollection<IWebElement> elements = this.driver.FindElements(by);
            this.OnFindElementCompleted(e);
            foreach (IWebElement element in elements)
            {
                IWebElement wrappedElement = this.WrapElement(element);
                wrappedElementList.Add(wrappedElement);
            }

            return wrappedElementList.AsReadOnly();
        }
        #endregion

        #region IDisposable Members
        /// <summary>
        /// Frees all managed and unmanaged resources used by this instance.
        /// </summary>
        public void Dispose()
        {
            this.Dispose(true);
            GC.SuppressFinalize(this);
        }
        #endregion

        #region IJavaScriptExecutor Methods
        /// <summary>
        /// Executes JavaScript in the context of the currently selected frame or window.
        /// </summary>
        /// <param name="script">The JavaScript code to execute.</param>
        /// <param name="args">The arguments to the script.</param>
        /// <returns>The value returned by the script.</returns>
        /// <remarks>
        /// <para>
        /// The <see cref="ExecuteScript"/>method executes JavaScript in the context of 
        /// the currently selected frame or window. This means that "document" will refer 
        /// to the current document. If the script has a return value, then the following 
        /// steps will be taken:
        /// </para>
        /// <para>
        /// <list type="bullet">
        /// <item><description>For an HTML element, this method returns a <see cref="IWebElement"/></description></item>
        /// <item><description>For a number, a <see cref="System.Int64"/> is returned</description></item>
        /// <item><description>For a boolean, a <see cref="System.Boolean"/> is returned</description></item>
        /// <item><description>For all other cases a <see cref="System.String"/> is returned.</description></item>
        /// <item><description>For an array,we check the first element, and attempt to return a 
        /// <see cref="List{T}"/> of that type, following the rules above. Nested lists are not
        /// supported.</description></item>
        /// <item><description>If the value is null or there is no return value,
        /// <see langword="null"/> is returned.</description></item>
        /// </list>
        /// </para>
        /// <para>
        /// Arguments must be a number (which will be converted to a <see cref="System.Int64"/>),
        /// a <see cref="System.Boolean"/>, a <see cref="System.String"/> or a <see cref="IWebElement"/>.
        /// An exception will be thrown if the arguments do not meet these criteria. 
        /// The arguments will be made available to the JavaScript via the "arguments" magic 
        /// variable, as if the function were called via "Function.apply" 
        /// </para>
        /// </remarks>
        public object ExecuteScript(string script, params object[] args)
        {
            IJavaScriptExecutor javascriptDriver = this.driver as IJavaScriptExecutor;
            if (javascriptDriver == null)
            {
                throw new NotSupportedException("Underlying driver instance does not support executing javascript");
            }

            object[] unwrappedArgs = UnwrapElementArguments(args);
            WebDriverScriptEventArgs e = new WebDriverScriptEventArgs(this.driver, script);
            this.OnScriptExecuting(e);
            object scriptResult = javascriptDriver.ExecuteScript(script, unwrappedArgs);
            this.OnScriptExecuted(e);
            return scriptResult;
        }

        /// <summary>
        /// Executes JavaScript asynchronously in the context of the currently selected frame or window.
        /// </summary>
        /// <param name="script">The JavaScript code to execute.</param>
        /// <param name="args">The arguments to the script.</param>
        /// <returns>The value returned by the script.</returns>
        public object ExecuteAsyncScript(string script, params object[] args)
        {
            IJavaScriptExecutor javascriptDriver = this.driver as IJavaScriptExecutor;
            if (javascriptDriver == null)
            {
                throw new NotSupportedException("Underlying driver instance does not support executing javascript");
            }

            object[] unwrappedArgs = UnwrapElementArguments(args);
            WebDriverScriptEventArgs e = new WebDriverScriptEventArgs(this.driver, script);
            this.OnScriptExecuting(e);
            object scriptResult = javascriptDriver.ExecuteAsyncScript(script, unwrappedArgs);
            this.OnScriptExecuted(e);
            return scriptResult;
        }
        #endregion

        #region ITakesScreenshot Members
        /// <summary>
        /// Gets a <see cref="Screenshot"/> object representing the image of the page on the screen.
        /// </summary>
        /// <returns>A <see cref="Screenshot"/> object containing the image.</returns>
        public Screenshot GetScreenshot()
        {
            ITakesScreenshot screenshotDriver = this.driver as ITakesScreenshot;
            if (this.driver == null)
            {
                throw new NotSupportedException("Underlying driver instance does not support taking screenshots");
            }

            Screenshot screen = null;
            screen = screenshotDriver.GetScreenshot();
            return screen;
        }
        #endregion

        /// <summary>
        /// Frees all managed and, optionally, unmanaged resources used by this instance.
        /// </summary>
        /// <param name="disposing"><see langword="true"/> to dispose of only managed resources;
        /// <see langword="false"/> to dispose of managed and unmanaged resources.</param>
        protected virtual void Dispose(bool disposing)
        {
            if (disposing)
            {
                this.driver.Dispose();
            }
        }

        /// <summary>
        /// Raises the <see cref="Navigating"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebDriverNavigationEventArgs"/> that contains the event data.</param>
        protected virtual void OnNavigating(WebDriverNavigationEventArgs e)
        {
            if (this.Navigating != null)
            {
                this.Navigating(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="Navigated"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebDriverNavigationEventArgs"/> that contains the event data.</param>
        protected virtual void OnNavigated(WebDriverNavigationEventArgs e)
        {
            if (this.Navigated != null)
            {
                this.Navigated(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="NavigatingBack"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebDriverNavigationEventArgs"/> that contains the event data.</param>
        protected virtual void OnNavigatingBack(WebDriverNavigationEventArgs e)
        {
            if (this.NavigatingBack != null)
            {
                this.NavigatingBack(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="NavigatedBack"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebDriverNavigationEventArgs"/> that contains the event data.</param>
        protected virtual void OnNavigatedBack(WebDriverNavigationEventArgs e)
        {
            if (this.NavigatedBack != null)
            {
                this.NavigatedBack(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="NavigatingForward"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebDriverNavigationEventArgs"/> that contains the event data.</param>
        protected virtual void OnNavigatingForward(WebDriverNavigationEventArgs e)
        {
            if (this.NavigatingForward != null)
            {
                this.NavigatingForward(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="NavigatedForward"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebDriverNavigationEventArgs"/> that contains the event data.</param>
        protected virtual void OnNavigatedForward(WebDriverNavigationEventArgs e)
        {
            if (this.NavigatedForward != null)
            {
                this.NavigatedForward(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="ElementClicking"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebElementEventArgs"/> that contains the event data.</param>
        protected virtual void OnElementClicking(WebElementEventArgs e)
        {
            if (this.ElementClicking != null)
            {
                this.ElementClicking(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="ElementClicked"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebElementEventArgs"/> that contains the event data.</param>
        protected virtual void OnElementClicked(WebElementEventArgs e)
        {
            if (this.ElementClicked != null)
            {
                this.ElementClicked(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="ElementValueChanging"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebElementEventArgs"/> that contains the event data.</param>
        protected virtual void OnElementValueChanging(WebElementEventArgs e)
        {
            if (this.ElementValueChanging != null)
            {
                this.ElementValueChanging(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="ElementValueChanged"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebElementEventArgs"/> that contains the event data.</param>
        protected virtual void OnElementValueChanged(WebElementEventArgs e)
        {
            if (this.ElementValueChanged != null)
            {
                this.ElementValueChanged(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="FindingElement"/> event.
        /// </summary>
        /// <param name="e">A <see cref="FindElementEventArgs"/> that contains the event data.</param>
        protected virtual void OnFindingElement(FindElementEventArgs e)
        {
            if (this.FindingElement != null)
            {
                this.FindingElement(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="FindElementCompleted"/> event.
        /// </summary>
        /// <param name="e">A <see cref="FindElementEventArgs"/> that contains the event data.</param>
        protected virtual void OnFindElementCompleted(FindElementEventArgs e)
        {
            if (this.FindElementCompleted != null)
            {
                this.FindElementCompleted(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="ScriptExecuting"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebDriverScriptEventArgs"/> that contains the event data.</param>
        protected virtual void OnScriptExecuting(WebDriverScriptEventArgs e)
        {
            if (this.ScriptExecuting != null)
            {
                this.ScriptExecuting(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="ScriptExecuted"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebDriverScriptEventArgs"/> that contains the event data.</param>
        protected virtual void OnScriptExecuted(WebDriverScriptEventArgs e)
        {
            if (this.ScriptExecuted != null)
            {
                this.ScriptExecuted(this, e);
            }
        }

        /// <summary>
        /// Raises the <see cref="ExceptionThrown"/> event.
        /// </summary>
        /// <param name="e">A <see cref="WebDriverExceptionEventArgs"/> that contains the event data.</param>
        protected virtual void OnException(WebDriverExceptionEventArgs e)
        {
            if (this.ExceptionThrown != null)
            {
                this.ExceptionThrown(this, e);
            }
        }

        private static object[] UnwrapElementArguments(object[] args)
        {
            // Walk the args: the various drivers expect unwrapped versions of the elements
            List<object> unwrappedArgs = new List<object>();
            foreach (object arg in args)
            {
                EventFiringWebElement eventElementArg = arg as EventFiringWebElement;
                if (eventElementArg != null)
                {
                    unwrappedArgs.Add(eventElementArg.WrappedElement);
                }
                else
                {
                    unwrappedArgs.Add(arg);
                }
            }

            return unwrappedArgs.ToArray();
        }

        private IWebElement WrapElement(IWebElement underlyingElement)
        {
            IWebElement wrappedElement = null;
            IRenderedWebElement renderedElement = underlyingElement as IRenderedWebElement;
            if (renderedElement != null)
            {
                wrappedElement = new EventFiringRenderedWebElement(this, underlyingElement);
            }
            else
            {
                wrappedElement = new EventFiringWebElement(this, underlyingElement);
            }

            return wrappedElement;
        }

        /// <summary>
        /// Provides a mechanism for Navigating with the driver.
        /// </summary>
        private class EventFiringNavigation : INavigation
        {
            private EventFiringWebDriver parentDriver;
            private INavigation wrappedNavigation;

            /// <summary>
            /// Initializes a new instance of the EventFiringNavigation class
            /// </summary>
            /// <param name="driver">Driver in use</param>
            public EventFiringNavigation(EventFiringWebDriver driver)
            {
                this.parentDriver = driver;
                this.wrappedNavigation = this.parentDriver.WrappedDriver.Navigate();
            }

            #region INavigation Members
            /// <summary>
            /// Move the browser back
            /// </summary>
            public void Back()
            {
                WebDriverNavigationEventArgs e = new WebDriverNavigationEventArgs(this.parentDriver);
                this.parentDriver.OnNavigatingBack(e);
                this.wrappedNavigation.Back();
                this.parentDriver.OnNavigatedBack(e);
            }

            /// <summary>
            /// Move the browser forward
            /// </summary>
            public void Forward()
            {
                WebDriverNavigationEventArgs e = new WebDriverNavigationEventArgs(this.parentDriver);
                this.parentDriver.OnNavigatingForward(e);
                this.wrappedNavigation.Forward();
                this.parentDriver.OnNavigatedForward(e);
            }

            /// <summary>
            /// Navigate to a url for your test
            /// </summary>
            /// <param name="url">String of where you want the browser to go to</param>
            public void GoToUrl(string url)
            {
                WebDriverNavigationEventArgs e = new WebDriverNavigationEventArgs(this.parentDriver, url);
                this.parentDriver.OnNavigating(e);
                this.wrappedNavigation.GoToUrl(url);
                this.parentDriver.OnNavigated(e);
            }

            /// <summary>
            /// Navigate to a url for your test
            /// </summary>
            /// <param name="url">Uri object of where you want the browser to go to</param>
            public void GoToUrl(Uri url)
            {
                WebDriverNavigationEventArgs e = new WebDriverNavigationEventArgs(this.parentDriver, url.ToString());
                this.parentDriver.OnNavigating(e);
                this.wrappedNavigation.GoToUrl(url);
                this.parentDriver.OnNavigated(e);
            }

            /// <summary>
            /// Refresh the browser
            /// </summary>
            public void Refresh()
            {
                this.wrappedNavigation.Refresh();
            }

            #endregion
        }

        /// <summary>
        /// Provides a mechanism for setting options needed for the driver during the test.
        /// </summary>
        private class EventFiringOptions : IOptions
        {
            private IOptions wrappedOptions;

            /// <summary>
            /// Initializes a new instance of the EventFiringOptions class
            /// </summary>
            /// <param name="driver">Instance of the driver currently in use</param>
            public EventFiringOptions(EventFiringWebDriver driver)
            {
                this.wrappedOptions = driver.Manage();
            }

            #region IOptions Members
            /// <summary>
            /// Gets or sets the speed with which actions are executed in the browser.
            /// </summary>
            public Speed Speed
            {
                get
                {
                    return this.wrappedOptions.Speed;
                }

                set
                {
                    this.wrappedOptions.Speed = value;
                }
            }

            /// <summary>
            /// Method for creating a cookie in the browser
            /// </summary>
            /// <param name="cookie"><see cref="Cookie"/> that represents a cookie in the browser</param>
            public void AddCookie(Cookie cookie)
            {
                this.wrappedOptions.AddCookie(cookie);
            }

            /// <summary>
            /// Method for getting a Collection of Cookies that are present in the browser
            /// </summary>
            /// <returns>ReadOnlyCollection of Cookies in the browser</returns>
            public ReadOnlyCollection<Cookie> GetCookies()
            {
                return this.wrappedOptions.GetCookies();
            }

            /// <summary>
            /// Method for returning a getting a cookie by name
            /// </summary>
            /// <param name="name">name of the cookie that needs to be returned</param>
            /// <returns>A Cookie from the name</returns>
            public Cookie GetCookieNamed(string name)
            {
                return this.wrappedOptions.GetCookieNamed(name);
            }

            /// <summary>
            /// Delete a cookie in the browser by passing in a copy of a cookie
            /// </summary>
            /// <param name="cookie">An object that represents a copy of the cookie that needs to be deleted</param>
            public void DeleteCookie(Cookie cookie)
            {
                this.wrappedOptions.DeleteCookie(cookie);
            }

            /// <summary>
            /// Delete the cookie by passing in the name of the cookie
            /// </summary>
            /// <param name="name">The name of the cookie that is in the browser</param>
            public void DeleteCookieNamed(string name)
            {
                this.wrappedOptions.DeleteCookieNamed(name);
            }

            /// <summary>
            /// Delete All Cookies that are present in the browser
            /// </summary>
            public void DeleteAllCookies()
            {
                this.wrappedOptions.DeleteAllCookies();
            }

            /// <summary>
            /// Provides access to the timeouts defined for this driver.
            /// </summary>
            /// <returns>An object implementing the <see cref="ITimeouts"/> interface.</returns>
            public ITimeouts Timeouts()
            {
                return new EventFiringTimeouts(this.wrappedOptions);
            }

            #endregion
        }

        /// <summary>
        /// Provides a mechanism for finding elements on the page with locators.
        /// </summary>
        private class EventFiringTargetLocator : ITargetLocator
        {
            private ITargetLocator wrappedLocator;

            /// <summary>
            /// Initializes a new instance of the EventFiringTargetLocator class
            /// </summary>
            /// <param name="driver">The driver that is currently in use</param>
            public EventFiringTargetLocator(EventFiringWebDriver driver)
            {
                this.wrappedLocator = driver.SwitchTo();
            }

            #region ITargetLocator Members
            /// <summary>
            /// Move to a different frame using its index
            /// </summary>
            /// <param name="frameIndex">The index of the </param>
            /// <returns>A WebDriver instance that is currently in use</returns>
            public IWebDriver Frame(int frameIndex)
            {
                return this.wrappedLocator.Frame(frameIndex);
            }

            /// <summary>
            /// Move to different frame using its name
            /// </summary>
            /// <param name="frameName">name of the frame</param>
            /// <returns>A WebDriver instance that is currently in use</returns>
            public IWebDriver Frame(string frameName)
            {
                return this.wrappedLocator.Frame(frameName);
            }

            /// <summary>
            /// Move to a frame element.
            /// </summary>
            /// <param name="frameElement">a previously found FRAME or IFRAME element.</param>
            /// <returns>A WebDriver instance that is currently in use.</returns>
            public IWebDriver Frame(IWebElement frameElement)
            {
                return this.wrappedLocator.Frame(frameElement);
            }

            /// <summary>
            /// Change to the Window by passing in the name
            /// </summary>
            /// <param name="windowName">name of the window that you wish to move to</param>
            /// <returns>A WebDriver instance that is currently in use</returns>
            public IWebDriver Window(string windowName)
            {
                return this.wrappedLocator.Window(windowName);
            }

            /// <summary>
            /// Change the active frame to the default 
            /// </summary>
            /// <returns>Element of the default</returns>
            public IWebDriver DefaultContent()
            {
                return this.wrappedLocator.DefaultContent();
            }

            /// <summary>
            /// Finds the active element on the page and returns it
            /// </summary>
            /// <returns>Element that is active</returns>
            public IWebElement ActiveElement()
            {
                return this.wrappedLocator.ActiveElement();
            }

            /// <summary>
            /// Switches to the currently active modal dialog for this particular driver instance.
            /// </summary>
            /// <returns>A handle to the dialog.</returns>
            public IAlert Alert()
            {
                return this.wrappedLocator.Alert();
            }

            #endregion
        }

        /// <summary>
        /// Defines the interface through which the user can define timeouts.
        /// </summary>
        private class EventFiringTimeouts : ITimeouts
        {
            private ITimeouts wrappedTimeouts;

            /// <summary>
            /// Initializes a new instance of the EventFiringTimeouts class
            /// </summary>
            /// <param name="options">The <see cref="IOptions"/> object to wrap.</param>
            public EventFiringTimeouts(IOptions options)
            {
                this.wrappedTimeouts = options.Timeouts();
            }

            #region ITimeouts Members
            /// <summary>
            /// Specifies the amount of time the driver should wait when searching for an
            /// element if it is not immediately present.
            /// </summary>
            /// <param name="timeToWait">A <see cref="TimeSpan"/> structure defining the amount of time to wait.</param>
            /// <returns>A self reference</returns>
            /// <remarks>
            /// When searching for a single element, the driver should poll the page
            /// until the element has been found, or this timeout expires before throwing
            /// a <see cref="NoSuchElementException"/>. When searching for multiple elements,
            /// the driver should poll the page until at least one element has been found
            /// or this timeout has expired.
            /// <para>
            /// Increasing the implicit wait timeout should be used judiciously as it
            /// will have an adverse effect on test run time, especially when used with
            /// slower location strategies like XPath.
            /// </para>
            /// </remarks>
            public ITimeouts ImplicitlyWait(TimeSpan timeToWait)
            {
                return this.wrappedTimeouts.ImplicitlyWait(timeToWait);
            }

            /// <summary>
            /// Specifies the amount of time the driver should wait when executing JavaScript asynchronously.
            /// </summary>
            /// <param name="timeToWait">A <see cref="TimeSpan"/> structure defining the amount of time to wait.</param>
            /// <returns>A self reference</returns>
            public ITimeouts SetScriptTimeout(TimeSpan timeToWait)
            {
                return this.wrappedTimeouts.SetScriptTimeout(timeToWait);
            }

            #endregion
        }

        /// <summary>
        /// EventFiringWebElement allows you to have access to specific items that are found on the page
        /// </summary>
        private class EventFiringWebElement : IWebElement, IWrapsElement
        {
            private IWebElement underlyingElement;
            private EventFiringWebDriver parentDriver;

            /// <summary>
            /// Initializes a new instance of the <see cref="EventFiringWebElement"/> class.
            /// </summary>
            /// <param name="driver">The <see cref="EventFiringWebDriver"/> instance hosting this element.</param>
            /// <param name="element">The <see cref="IWebElement"/> to wrap for event firing.</param>
            public EventFiringWebElement(EventFiringWebDriver driver, IWebElement element)
            {
                this.underlyingElement = element;
                this.parentDriver = driver;
            }

            #region IWrapsElement Members
            /// <summary>
            /// Gets the underlying wrapped <see cref="IWebElement"/>.
            /// </summary>
            public IWebElement WrappedElement
            {
                get { return this.underlyingElement; }
            }
            #endregion

            #region IWebElement Members
            /// <summary>
            /// Gets the DOM Tag of element
            /// </summary>
            public string TagName
            {
                get { return this.underlyingElement.TagName; }
            }

            /// <summary>
            /// Gets the text from the element
            /// </summary>
            public string Text
            {
                get { return this.underlyingElement.Text; }
            }

            /// <summary>
            /// Gets the value of the element's "value" attribute. If this value has been modified after the page has loaded (for example, through javascript) then this will reflect the current value of the "value" attribute.
            /// </summary>
            public string Value
            {
                get { return this.underlyingElement.Value; }
            }

            /// <summary>
            /// Gets a value indicating whether an element is currently enabled
            /// </summary>
            public bool Enabled
            {
                get { return this.underlyingElement.Enabled; }
            }

            /// <summary>
            /// Gets a value indicating whether this element is selected or not. This operation only applies to input elements such as checkboxes, options in a select and radio buttons.
            /// </summary>
            public bool Selected
            {
                get { return this.underlyingElement.Selected; }
            }

            /// <summary>
            /// Method to clear the text out of an Input element
            /// </summary>
            public void Clear()
            {
                WebElementEventArgs e = new WebElementEventArgs(this.parentDriver.WrappedDriver, this.underlyingElement);
                this.parentDriver.OnElementValueChanging(e);
                this.underlyingElement.Clear();
                this.parentDriver.OnElementValueChanged(e);
            }

            /// <summary>
            /// Method for sending native key strokes to the browser
            /// </summary>
            /// <param name="text">String containing what you would like to type onto the screen</param>
            public void SendKeys(string text)
            {
                WebElementEventArgs e = new WebElementEventArgs(this.parentDriver.WrappedDriver, this.underlyingElement);
                this.parentDriver.OnElementValueChanging(e);
                this.underlyingElement.SendKeys(text);
                this.parentDriver.OnElementValueChanged(e);
            }

            /// <summary>
            /// If this current element is a form, or an element within a form, then this will be submitted to the remote server. 
            /// If this causes the current page to change, then this method will block until the new page is loaded.
            /// </summary>
            public void Submit()
            {
                this.underlyingElement.Submit();
            }

            /// <summary>
            /// Click this element. If this causes a new page to load, this method will block until the page has loaded. At this point, you should discard all references to this element and any further operations performed on this element 
            /// will have undefined behaviour unless you know that the element and the page will still be present. If this element is not clickable, then this operation is a no-op since it's pretty common for someone to accidentally miss 
            /// the target when clicking in Real Life
            /// </summary>
            public void Click()
            {
                WebElementEventArgs e = new WebElementEventArgs(this.parentDriver.WrappedDriver, this.underlyingElement);
                this.parentDriver.OnElementClicking(e);
                this.underlyingElement.Click();
                this.parentDriver.OnElementClicked(e);
            }

            /// <summary>
            /// Select or unselect element. This operation only applies to input elements such as checkboxes, options in a select and radio buttons.
            /// </summary>
            public void Select()
            {
                this.underlyingElement.Select();
            }

            /// <summary>
            /// If this current element is a form, or an element within a form, then this will be submitted to the remote server. If this causes the current page to change, then this method will block until the new page is loaded.
            /// </summary>
            /// <param name="attributeName">Attribute you wish to get details of</param>
            /// <returns>The attribute's current value or null if the value is not set.</returns>
            public string GetAttribute(string attributeName)
            {
                return this.underlyingElement.GetAttribute(attributeName);
            }

            /// <summary>
            /// If the element is a checkbox this will toggle the elements state from selected to not selected, or from not selected to selected
            /// </summary>
            /// <returns>Whether the toggled element is selected (true) or not (false) after this toggle is complete</returns>
            public bool Toggle()
            {
                WebElementEventArgs e = new WebElementEventArgs(this.parentDriver.WrappedDriver, this.underlyingElement);
                this.parentDriver.OnElementValueChanging(e);
                bool toggleValue = this.underlyingElement.Toggle();
                this.parentDriver.OnElementValueChanged(e);
                return toggleValue;
            }

            #endregion

            #region ISearchContext Members
            /// <summary>
            /// Finds the first element in the page that matches the <see cref="By"/> object
            /// </summary>
            /// <param name="by">By mechanism to find the element</param>
            /// <returns>IWebElement object so that you can interction that object</returns>
            public IWebElement FindElement(By by)
            {
                FindElementEventArgs e = new FindElementEventArgs(this.parentDriver.WrappedDriver, this.underlyingElement, by);
                this.parentDriver.OnFindingElement(e);
                IWebElement element = this.underlyingElement.FindElement(by);
                this.parentDriver.OnFindElementCompleted(e);
                IWebElement wrappedElement = this.parentDriver.WrapElement(element);
                return wrappedElement;
            }

            /// <summary>
            /// Finds the elements on the page by using the <see cref="By"/> object and returns a ReadOnlyCollection of the Elements on the page
            /// </summary>
            /// <param name="by">By mechanism to find the element</param>
            /// <returns>ReadOnlyCollection of IWebElement</returns>
            public ReadOnlyCollection<IWebElement> FindElements(By by)
            {
                List<IWebElement> wrappedElementList = new List<IWebElement>();
                FindElementEventArgs e = new FindElementEventArgs(this.parentDriver.WrappedDriver, this.underlyingElement, by);
                this.parentDriver.OnFindingElement(e);
                ReadOnlyCollection<IWebElement> elements = this.underlyingElement.FindElements(by);
                this.parentDriver.OnFindElementCompleted(e);
                foreach (IWebElement element in elements)
                {
                    IWebElement wrappedElement = this.parentDriver.WrapElement(element);
                    wrappedElementList.Add(wrappedElement);
                }

                return wrappedElementList.AsReadOnly();
            }

            #endregion
        }

        /// <summary>
        /// Provides a mechanism to find Rendered Elements on the page
        /// </summary>
        private class EventFiringRenderedWebElement : EventFiringWebElement, IRenderedWebElement
        {
            /// <summary>
            /// Initializes a new instance of the <see cref="EventFiringRenderedWebElement"/> class.
            /// </summary>
            /// <param name="driver">The <see cref="EventFiringWebDriver"/> instance hosting this element.</param>
            /// <param name="element">The <see cref="IWebElement"/> to wrap for event firing.</param>
            public EventFiringRenderedWebElement(EventFiringWebDriver driver, IWebElement element)
                : base(driver, element)
            {
            }

            #region IRenderedWebElement Members
            /// <summary>
            /// Gets the Location of an element and returns a Point object
            /// </summary>
            public Point Location
            {
                get
                {
                    IRenderedWebElement renderedElement = WrappedElement as IRenderedWebElement;
                    return renderedElement.Location;
                }
            }

            /// <summary>
            /// Gets the <see cref="Size"/> of the element on the page
            /// </summary>
            public Size Size
            {
                get
                {
                    IRenderedWebElement renderedElement = WrappedElement as IRenderedWebElement;
                    return renderedElement.Size;
                }
            }

            /// <summary>
            /// Gets a value indicating whether the element is currently being displayed
            /// </summary>
            public bool Displayed
            {
                get
                {
                    IRenderedWebElement renderedElement = WrappedElement as IRenderedWebElement;
                    return renderedElement.Displayed;
                }
            }

            /// <summary>
            /// Method to return the value of a CSS Property
            /// </summary>
            /// <param name="propertyName">CSS property key</param>
            /// <returns>string value of the CSS property</returns>
            public string GetValueOfCssProperty(string propertyName)
            {
                IRenderedWebElement renderedElement = WrappedElement as IRenderedWebElement;
                return renderedElement.GetValueOfCssProperty(propertyName);
            }

            /// <summary>
            /// Moves the mouse over the element to do a hover
            /// </summary>
            public void Hover()
            {
                IRenderedWebElement renderedElement = WrappedElement as IRenderedWebElement;
                renderedElement.Hover();
            }

            /// <summary>
            /// Move to an element, MouseDown on the element and move it by passing in the how many pixels horizontally and vertically you wish to move it
            /// </summary>
            /// <param name="moveRightBy">Integer to move it left or right</param>
            /// <param name="moveDownBy">Integer to move it up or down</param>
            public void DragAndDropBy(int moveRightBy, int moveDownBy)
            {
                IRenderedWebElement renderedElement = WrappedElement as IRenderedWebElement;
                renderedElement.DragAndDropBy(moveRightBy, moveDownBy);
            }

            /// <summary>
            /// Drag and Drop an element to another element
            /// </summary>
            /// <param name="element">Element you wish to drop on</param>
            public void DragAndDropOn(IRenderedWebElement element)
            {
                IRenderedWebElement renderedElement = WrappedElement as IRenderedWebElement;
                renderedElement.DragAndDropOn(element);
            }

            #endregion
        }
    }
}
