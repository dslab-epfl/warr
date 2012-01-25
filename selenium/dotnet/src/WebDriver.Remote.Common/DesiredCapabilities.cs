﻿using System;
using System.Collections.Generic;
using System.Globalization;
using Newtonsoft.Json;

namespace OpenQA.Selenium.Remote
{
    /// <summary>
    /// Class to Create the capabilities of the browser you require for <see cref="IWebDriver"/>. 
    /// If you wish to use default values use the static methods
    /// </summary>
    public class DesiredCapabilities : ICapabilities
    {
        private readonly Dictionary<string, object> customCapabilities = new Dictionary<string, object>();

        private string name;
        private string browserVersion;
        private Platform browserPlatform;
        private bool javascriptEnabled;

        /// <summary>
        /// Initializes a new instance of the DesiredCapabilities class
        /// </summary>
        /// <param name="browser">Name of the browser e.g. firefox, internet explorer, safari</param>
        /// <param name="version">Version of the browser</param>
        /// <param name="platform">The platform it works on</param>
        public DesiredCapabilities(string browser, string version, Platform platform)
        {
            this.name = browser;
            this.browserVersion = version;
            this.browserPlatform = platform;
        }

        /// <summary>
        /// Initializes a new instance of the DesiredCapabilities class
        /// </summary>
        public DesiredCapabilities()
        {
        }

        /// <summary>
        /// Initializes a new instance of the DesiredCapabilities class
        /// </summary>
        /// <param name="rawMap">Dictionary of items for the remotedriver</param>
        /// <example>
        /// <code>
        /// DesiredCapabilities capabilities = new DesiredCapabilities(new Dictionary<![CDATA[<string,object>]]>(){["browserName","firefox"],["version",string.Empty],["javaScript",true]});
        /// </code>
        /// </example>
        public DesiredCapabilities(Dictionary<string, object> rawMap)
        {
            this.name = (string)rawMap["browserName"];
            this.browserVersion = (string)rawMap["version"];
            this.javascriptEnabled = (bool)rawMap["javascriptEnabled"];

            if (rawMap.ContainsKey("platform"))
            {
                object raw = rawMap["platform"];
                string rawAsString = raw as string;
                Platform rawAsPlatform = raw as Platform;
                if (rawAsString != null)
                {
                    PlatformType platformInfo = (PlatformType)Enum.Parse(typeof(PlatformType), rawAsString, true);
                    this.browserPlatform = new Platform(platformInfo);
                }
                else if (rawAsPlatform != null)
                {
                    this.browserPlatform = rawAsPlatform;
                }
            }

            List<string> knownCapabilities = new List<string> { "browserName", "version", "javascriptEnabled", "platform" };
            foreach (string key in rawMap.Keys)
            {
                if (!knownCapabilities.Contains(key))
                {
                    this.customCapabilities[key] = rawMap[key];
                }
            }
        }

        /// <summary>
        /// Gets the browser name 
        /// </summary>
        [JsonProperty("browserName")]
        public string BrowserName
        {
            get { return this.name; }
        }

        /// <summary>
        /// Gets or sets the platform
        /// </summary>
        [JsonProperty("platform")]
        public Platform Platform
        {
            get { return this.browserPlatform; }
            set { this.browserPlatform = value; }
        }

        /// <summary>
        /// Gets the browser version
        /// </summary>
        [JsonProperty("version")]
        public string Version
        {
            get { return this.browserVersion; }
        }

        /// <summary>
        /// Gets or sets a value indicating whether the browser is javascript enabled
        /// </summary>
        [JsonProperty("javascriptEnabled")]
        public bool IsJavaScriptEnabled
        {
            get { return this.javascriptEnabled; }
            set { this.javascriptEnabled = value; }
        }

        /// <summary>
        /// Method to return a new DesiredCapabilities using defaults
        /// </summary>
        /// <returns>New instance of DesiredCapabilites for use with Firefox</returns>
        public static DesiredCapabilities Firefox()
        {
            return new DesiredCapabilities("firefox", string.Empty, new Platform(PlatformType.Any));
        }

        /// <summary>
        /// Method to return a new DesiredCapabilities using defaults
        /// </summary>
        /// <returns>New instance of DesiredCapabilities for use with Internet Explorer</returns>
        public static DesiredCapabilities InternetExplorer()
        {
            return new DesiredCapabilities("internet explorer", string.Empty, new Platform(PlatformType.Windows));
        }

        /// <summary>
        /// Method to return a new DesiredCapabilities using defaults
        /// </summary>
        /// <returns>New instance of DesiredCapabilities for use with HTMLUnit</returns>
        public static DesiredCapabilities HtmlUnit()
        {
            return new DesiredCapabilities("htmlunit", string.Empty, new Platform(PlatformType.Any));
        }
		
		/// <summary>
        /// Method to return a new DesiredCapabilities using defaults
        /// </summary>
        /// <returns>New instance of DesiredCapabilities for use with HTMLUnit with JS</returns>
        public static DesiredCapabilities HtmlUnitWithJavaScript()
        {
            return new DesiredCapabilities("htmlunit", "firefox", new Platform(PlatformType.Any));
        }

        /// <summary>
        /// Method to return a new DesiredCapabilities using defaults
        /// </summary>
        /// <returns>New instance of DesiredCapabilities for use with IPhone</returns>
        public static DesiredCapabilities IPhone()
        {
            return new DesiredCapabilities("iphone", string.Empty, new Platform(PlatformType.MacOSX));
        }

        /// <summary>
        /// Method to return a new DesiredCapabilities using defaults
        /// </summary>
        /// <returns>New istance of DesiredCapabilities for use with Chrome</returns>
        public static DesiredCapabilities Chrome()
        {
            // This is strangely inconsistent.
            DesiredCapabilities dc = new DesiredCapabilities("chrome", string.Empty, new Platform(PlatformType.Windows));
            dc.IsJavaScriptEnabled = true;
            return dc;
        }

        /// <summary>
        /// Method to return a new DesiredCapabilities using defaults
        /// </summary>
        /// <returns>New istance of DesiredCapabilities for use with Android</returns>
        public static DesiredCapabilities Android()
        {
            return new DesiredCapabilities("android", string.Empty, new Platform(PlatformType.Linux));
        }

        /// <summary>
        /// Gets a value indicating whether the browser has a given capability.
        /// </summary>
        /// <param name="capability">The capability ot get.</param>
        /// <returns>Returns <see langword="true"/> if the browser has the capability; otherwise, <see langword="false"/>.</returns>
        public bool HasCapability(string capability)
        {
            return this.customCapabilities.ContainsKey(capability);
        }

        /// <summary>
        /// Gets a capability of the browser.
        /// </summary>
        /// <param name="capability">The capability to get.</param>
        /// <returns>An object associated with the capability, or <see langword="null"/>
        /// if the capability is not set on the browser.</returns>
        public object GetCapability(string capability)
        {
            object capabilityValue = null;
            if (this.customCapabilities.ContainsKey(capability))
            {
                capabilityValue = this.customCapabilities[capability];
            }

            return capabilityValue;
        }

        /// <summary>
        /// Return HashCode for the DesiredCapabilties that has been created
        /// </summary>
        /// <returns>Integer of HashCode generated</returns>
        public override int GetHashCode()
        {
            int result;
            result = this.name != null ? this.name.GetHashCode() : 0;
            result = (31 * result) + (this.browserVersion != null ? this.browserVersion.GetHashCode() : 0);
            result = (31 * result) + (this.browserPlatform != null ? this.browserPlatform.GetHashCode() : 0);
            result = (31 * result) + (this.javascriptEnabled ? 1 : 0);
            return result;
        }

        /// <summary>
        /// Return a string of capabilies being used
        /// </summary>
        /// <returns>String of capabilites being used</returns>
        public override string ToString()
        {
            return string.Format(CultureInfo.InvariantCulture, "Capabilities [BrowserName={0}, IsJavaScriptEnabled={1}, Platform={2}, Version={3}]", this.name, this.javascriptEnabled, this.browserPlatform.PlatformType.ToString(), this.browserVersion);
        }

        /// <summary>
        /// Compare two DesiredCapabilities and will return either true or false
        /// </summary>
        /// <param name="obj">DesiredCapabities you wish to compare</param>
        /// <returns>true if they are the same or false if they are not</returns>
        public override bool Equals(object obj)
        {
            if (this == obj)
            {
                return true;
            }

            DesiredCapabilities other = obj as DesiredCapabilities;
            if (other == null)
            {
                return false;
            }

            if (this.javascriptEnabled != other.javascriptEnabled)
            {
                return false;
            }

            if (this.name != null ? this.name != other.name : other.name != null)
            {
                return false;
            }

            if (!this.browserPlatform.IsPlatformType(other.Platform.PlatformType))
            {
                return false;
            }

            if (this.browserVersion != null ? this.browserVersion != other.browserVersion : other.browserVersion != null)
            {
                return false;
            }

            return true;
        }
    }
}
