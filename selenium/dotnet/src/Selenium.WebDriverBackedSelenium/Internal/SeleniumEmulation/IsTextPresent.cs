using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;
using OpenQA.Selenium;

namespace Selenium.Internal.SeleniumEmulation
{
    /// <summary>
    /// Defines the command for the isTextPresent keyword.
    /// </summary>
    internal class IsTextPresent : SeleneseCommand
    {
        private static readonly Regex TextMatchingStrategyAndValueRegex = new Regex("^([a-zA-Z]+):(.*)");
        private static Dictionary<string, ITextMatchingStrategy> textMatchingStrategies = new Dictionary<string, ITextMatchingStrategy>();

        /// <summary>
        /// Initializes a new instance of the <see cref="IsTextPresent"/> class.
        /// </summary>
        public IsTextPresent()
        {
            SetUpTextMatchingStrategies();
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
            string text = string.Empty;
            IWebElement body = driver.FindElement(By.XPath("/html/body"));
            IJavaScriptExecutor executor = driver as IJavaScriptExecutor;
            if (executor == null)
            {
                text = body.Text;
            }
            else
            {
                text = JavaScriptLibrary.CallEmbeddedHtmlUtils(driver, "getTextContent", body).ToString();
            }

            text = text.Trim();

            string strategyName = "implicit";
            string use = locator;

            if (TextMatchingStrategyAndValueRegex.IsMatch(locator))
            {
                Match textMatch = TextMatchingStrategyAndValueRegex.Match(locator);
                strategyName = textMatch.Groups[1].Value;
                use = textMatch.Groups[2].Value;
            }

            ITextMatchingStrategy strategy = textMatchingStrategies[strategyName];
            return strategy.IsAMatch(use, text);
        }

        private static void SetUpTextMatchingStrategies()
        {
            if (textMatchingStrategies.Count == 0)
            {
                textMatchingStrategies.Add("implicit", new GlobTextMatchingStrategy());
                textMatchingStrategies.Add("glob", new GlobTextMatchingStrategy());
                textMatchingStrategies.Add("regexp", new RegexTextMatchingStrategy());
                textMatchingStrategies.Add("exact", new ExactTextMatchingStrategy());                
            }
        }
    }
}
