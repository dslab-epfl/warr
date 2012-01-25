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

package org.openqa.selenium.android;

import android.graphics.Point;
import android.util.Log;

import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows to access the DOM through Javascript DOM API.
 */
public class JavascriptDomAccessor {
  private static final String STALE = "_android_stale_element_";
  private static final String UNSELECTABLE = "_android_unselectable_element_";
  private static final String DISABLED = "_android_disabled_element_";
  private static final String FAILED = "_android_failed";
  private static final String UNSUPPORTED = "_android_unsupported";
  private static final String LOG_TAG = JavascriptDomAccessor.class.getName();
  private static final String NOT_VISIBLE = "notVisible";
  private static final int MAX_XPATH_ATTEMPTS = 5;
  private static final int XPATH_RETRY_TIMEOUT = 500; // milliseconds

  private final AndroidDriver driver;

  // TODO(berrada): Look at atoms in shared_js and reuse when possible.
  
  // This determines the context in which the Javascript is executed.
  // By default element id 0 respresents the document.
  private static final String CONTEXT_NODE = 
      "var contextNode = contextNode = doc.androiddriver_elements[arguments[1]];";

  private static final String ELEMENT_SIZE = 
      "var elementWidth = 0;" +
      "var elementHeight = 0;" +
      "if(element.getClientRects) {" +
      "  elementWidth = element.getClientRects()[0].width;" +
      "  elementHeight = element.getClientRects()[0].height;" +
      "} else {" +
      "  elementWidth = element.offsetWidth;" +
      "  elementHeight = element.offsetHeight;" +
      "}";
  
  // Adds an element to the cache if it does not already exists.
  public static final String ADD_TO_CACHE =
      "var indices = [];" +
      "for (var i = 0; i < result.length; i++) {" +
      "  var found = false;" +
      "  for (var e in doc.androiddriver_elements) {" +
      "    if (doc.androiddriver_elements[e] == result[i]) {" +
      "      found = true;" +
      "      indices.push(parseInt(e));" +
      "    }" +
      "  }" +
      "  if (found == false) {" +
      "    doc.androiddriver_next_id ++;" +
      "    doc.androiddriver_elements[doc.androiddriver_next_id] = result[i];" +
      "    indices.push(parseInt(doc.androiddriver_next_id));" +
      "  }" +
      "}";

  private static final String IS_SELECTED =
      "var isSelected = null;" +
      "var element = doc.androiddriver_elements[arguments[0]];" +
      "if (element.tagName.toLowerCase() == 'option') {" +
        "isSelected = element.selected;" +
      "} else if (element.tagName.toLowerCase() == 'input') {" +
        "isSelected = element.checked;" +
      "}";
  
  public JavascriptDomAccessor(AndroidDriver driver) {
    this.driver = driver;
  }
  
  /**
   * Gets an element using the id.
   * 
   * @param using the element id
   * @param elementId the element id in the Javascript cache
   * @return the element if found
   * @throws NoSuchElementException if the element is not found
   */
  public WebElement getElementById(String using, String elementId) {
    // If the current elementId is 0 the element reffers to the dom. Otherwise
    // the element reffers to a nested element in the dom.s
    if (!"0".equals(elementId)) {
      return getElementByXPath(".//*[@id='" + using + "']", elementId);
    }
    // TODO(berrada): by default do document.getElementById. Check "contains", and only 
	  // fall back to xpath if that fails. Closure has the ordering code (goog.dom, I think)
    String toExecute =
        initCacheJs() + 
        CONTEXT_NODE + 
        "var result = [];" +
        "if (contextNode.getElementById) {" +
        "  var element = document.getElementById(arguments[0]);" +
        "  if (element != null) {" +
        "    result.push(element);" +
        "  }" +
        "} else {" +
        installXPathJs() +
        // We use 5 for ResultType.ORDERED_NODE_ITERATOR_TYPE. This constant
        // is not defined in the JS API exposed for versions earlier than 2.0.
        "  var it = document.evaluate('.//*[@id=\\'' + arguments[0] + '\\']', contextNode, null, " +
            "5, null);" +
        "  var element = it.iterateNext();" +
        "  if (element == null) {" +
        "    return null;" +
        "  }" +
        "  result.push(element);" +
        "}" +
        ADD_TO_CACHE +
        "return indices;";
    List result = executeAndRetry(using, elementId, toExecute);
    List<WebElement> elements = createWebElementsWithIds(result, elementId);
    return getFirstElement(elements);
  }
  
  public List<WebElement> getElementsById(String using, String elementId) {
    return getElementsByXpath(".//*[@id = '" + using + "']", elementId);
  }
  
  public WebElement getElementByName(String using, String elementId) {
    if (!"0".equals(elementId)) { // nested elements
      return getElementByXPath(".//*[@name='" + using + "']", elementId);
    }
    @SuppressWarnings({"unchecked"})
    List<Integer> result = (List<Integer>) driver.executeScript(
        "var element = document.getElementsByName(arguments[0])[0];" +
        "var result = [];" +
        "if (element != null && element.length != 0) {" +
        "  result.push(element);" +
        "}" +
        initCacheJs() +
        ADD_TO_CACHE +
        "return indices;",
        using);
    List<WebElement> elements = createWebElementsWithIds(result, elementId);
    return getFirstElement(elements);
  }
 
  public List<WebElement> getElementsByName(String using, String elementId) {
    if (!"0".equals(elementId)) { // nested elements
      return getElementsByXpath(".//*[@name='" + using + "']", elementId);
    }
    @SuppressWarnings({"unchecked"})
    List<Integer> result = (List<Integer>) driver.executeScript(
        "var elements = document.getElementsByName(arguments[0]);" +
        "var result = [];" +
        "for (var i = 0; i < elements.length; i++) {" +
        "  result.push(elements[i]);" +
        "}" +
        initCacheJs() +
        ADD_TO_CACHE +
        "return indices;",
        using);
    return createWebElementsWithIds(result, elementId);
  }

  public WebElement getElementByTagName(String using, String elementId) {
    @SuppressWarnings({"unchecked"})
    List<Integer> result = (List<Integer>) driver.executeScript(
        initCacheJs() +
        CONTEXT_NODE +
        "var element = contextNode.getElementsByTagName(arguments[0])[0];" +
        "var result = [];" +
        "if (element != null && element.length != 0) {" +
        "  result.push(element);" +
        "}" +
        initCacheJs() +
        ADD_TO_CACHE +
        "return indices;",
        using, elementId);
    List<WebElement> elements = createWebElementsWithIds(result, elementId);
    return getFirstElement(elements);
  }
 
  public List<WebElement> getElementsByTagName(String using, String elementId) {
    @SuppressWarnings({"unchecked"})
    List<Integer> result = (List<Integer>) driver.executeScript(
        initCacheJs() +
        CONTEXT_NODE +
        "var elements = contextNode.getElementsByTagName(arguments[0]);" +
        "var result = [];" +
        "for (var i = 0; i < elements.length; i++) {" +
        "    result.push(elements[i]);" +
        "}" +
        initCacheJs() +
        ADD_TO_CACHE +
        "return indices;",
        using, elementId);
    return createWebElementsWithIds(result, elementId);
  }
  
  public WebElement getElementByXPath(String using, String elementId) {
    String toExecute =
        initCacheJs() +
        CONTEXT_NODE + 
        installXPathJs() +
        "var result = [];" +
        "var it = document.evaluate(arguments[0], contextNode, null, 5, null);" + 
        "var element = it.iterateNext();" +
        "if (element == null) {" +
        "  return null;" +
        "}" +
        "result.push(element);" +
        ADD_TO_CACHE +
        "return indices;";
    List result = executeAndRetry(using, elementId, toExecute);
    List<WebElement> elements = createWebElementsWithIds(result, elementId);
    return getFirstElement(elements);
  }
 
  public List<WebElement> getElementsByXpath(String using, String elementId) {
    String toExecute =
        initCacheJs() +
        CONTEXT_NODE +
        installXPathJs() +
        "var it = document.evaluate(arguments[0], contextNode, null, 5, null);" +
        "var result = [];" +
        "var element = it.iterateNext();" +
        "while (element) {" +
        "  result.push(element);" +
        "  element = it.iterateNext();" +
        "}" +
        ADD_TO_CACHE +
        "return indices;";
    List result = executeAndRetry(using, elementId, toExecute);
    return createWebElementsWithIds(result, elementId);
  }
 
  public WebElement getElementByLinkText(String using, String elementId) {
    @SuppressWarnings({"unchecked"})
    List<Integer> result = (List) driver.executeScript(
        initCacheJs() +
        CONTEXT_NODE +
        "var links = contextNode.getElementsByTagName('a');" +
        "var result = [];" +
        "for (var i = 0; i < links.length; i++) {" +
        "  if (links[i].innerText == arguments[0]) {" +
        "    result.push(links[i]);" +
        "    break;" +
        "  }" +
        "}" +
        ADD_TO_CACHE +
        "return indices;",
        using, elementId);
    List<WebElement> elements = createWebElementsWithIds(result, elementId);
    return getFirstElement(elements);
  }
 
  public List<WebElement> getElementsByLinkText(String using, String elementId) {
    @SuppressWarnings({"unchecked"})
    List<Integer> result = (List<Integer>) driver.executeScript(
        initCacheJs() +
        CONTEXT_NODE +
        "var links = contextNode.getElementsByTagName('a');" +
        "var result = [];" +
        "for (var i = 0; i < links.length; i++) {" +
        "  if (links[i].innerText == arguments[0]) {" +
        "    result.push(links[i]);" +
        "  }" +
        "}" +
        ADD_TO_CACHE +
        "return indices;",
        using, elementId);
    return createWebElementsWithIds(result, elementId);
  }
  
  public WebElement getElementByPartialLinkText(String using, String elementId) {
    @SuppressWarnings({"unchecked"})
    List<Integer> result = (List<Integer>) driver.executeScript(
        initCacheJs() +
        CONTEXT_NODE +
        "var links = contextNode.getElementsByTagName('a');" +
        "var result = [];" +
        "for (var i = 0; i < links.length; i++) {" +
        "  if (links[i].innerText.indexOf(arguments[0]) > -1) {" +
        "    result.push(links[i]);" +
        "    break;" +
        "  }" +
        "}" +
        ADD_TO_CACHE +
        "return indices;",
        using, elementId);
    List<WebElement> elements = createWebElementsWithIds(result, elementId);
    return getFirstElement(elements);
  }
  
  public List<WebElement> getElementsByPartialLinkText(String using, String elementId) {
    @SuppressWarnings({"unchecked"})
    List<Integer> result = (List<Integer>) driver.executeScript(
        initCacheJs() +
        CONTEXT_NODE +
        "var links = contextNode.getElementsByTagName('a');" +
        "var result = [];" +
        "for (var i = 0; i < links.length; i++) {" +
        "  if (links[i].innerText.indexOf(arguments[0]) > -1) {" +
        "    result.push(links[i]);" +
        "  }" +
        "}" +
        ADD_TO_CACHE +
        "return indices;",
        using, elementId);
    return createWebElementsWithIds(result, elementId);
  }
  
  public String getText(String elementId) {
    String result = (String)driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
        "  return element.innerText;" +
        "}" +
        "return 'stale';",
        elementId);
    throwExceptionIfFailed(result);
    return result;
  }

  public String getTagName(String elementId) {
    String result = (String) driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
        "  return element.tagName;" +
        "}" +
        "return 'stale';",
        elementId);
    throwExceptionIfFailed(result);
    return result;
  }

  /**
   * Returns comma separated X and Y coordinates of an element in the cache in a
   * {@link String} formatted as follow "x,y". 
   * 
   * @param elementId the element to return coordinates for
   * @return String containing the comma separated coordinate of the element
   */
  // TODO(berrada): Be aware that getBoundingClientRect only returns the visible area,
  // you may well need to add the offsets.
  public Point getCoordinate(String elementId) {
    String result = (String)driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
          getTopLeftCoordinatesJS() +
        "  return topLeftX + ',' + topLeftY;" +
        "}" +
        "return '" + STALE + "';",
        elementId);
    throwExceptionIfFailed(result);
    return parseCoordinate(result);
  }
  
  public void blur(String elementId) {
    String result = (String) driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
        "  element.blur();" +
        "  return true;" + 
        "}" +
        "return '" + STALE + "';",
        elementId);
    throwExceptionIfFailed(result);
  }
  
  public String getAttributeValue(String attribute, String elementId) {
    // TODO (berrada): This is equivalent to combining the "bot.dom.getProperty"
    // and "bot.dom.getAttribute" atoms.
    Object result = driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
        "  if (arguments[1] == 'selected' || arguments[1] == 'checked') {" +
           IS_SELECTED +
        "    return isSelected;" +
        "  }" +
        "  if (arguments[1] == 'disabled') {" +
        "    return element.disabled;" +
        "  }" +
        "  return " + ("value".equals(attribute) || attribute.startsWith("offset")
            || "index".equals(attribute) ?
                "element." + attribute + ";"
                : "element.getAttribute(arguments[1]);") +
        "}" +
        "return '" + STALE + "';",
        elementId, attribute);
    throwExceptionIfFailed(String.valueOf(result));
    return (result == null) ? null : String.valueOf(result);
  }

  public Point getSize(String elementId) {
    String result = (String) driver.executeScript(
      initCacheJs() +
      isElementStaleJs() +
      "if (isStale == false) {" +
        ELEMENT_SIZE +
        "return elementWidth + ',' + elementHeight;" +
      "}" +
      "return '" + STALE + "';",
      elementId);
    if (result == null) {
      return null;
    }
    throwExceptionIfFailed(result);    
    return parseCoordinate(result);
  }

  public void scrollIfNeeded(String elementId) {
    driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == true) {" +
        "  return '" + STALE + "';" +
        "}" +
        isDisplayedJs() +
        "if (isDisplayed == false) {" +
          "return '" + NOT_VISIBLE + "';" +
        "}" +
        getTopLeftCoordinatesJS() +
        "var xScroll = 0;" +
        "var yScroll = 0;" +
        "if (topLeftX < 0 || topLeftX > window.innerWidth) {" +
        "  xScroll = topLeftX - 20;" + // scroll horizontally
        "}" +
        "if (topLeftY < 0 || topLeftY > window.innerHeight) {" +
        "  yScroll = topLeftY - 20;" + // scroll vertically
        "}" +
        "if (xScroll != 0 || yScroll != 0) {" +
        "  window.scroll(xScroll, yScroll);" +
        "}", elementId);
  }
  
  /**
   * Computes the center coordinates of the element.
   * 
   * @param elementId
   * @return Point containing the screen coordinates of the element
   */
  public Point getCenterCoordinate(String elementId) {
    String result = (String) driver.executeScript(
      initCacheJs() +
      isElementStaleJs() +
      "if (isStale == true) {" +
      "  return '" + STALE + "';" +
      "}" +
      isDisplayedJs() +
      "if (isDisplayed == false) {" +
        "return '" + NOT_VISIBLE + "';" +
      "}" +
      ELEMENT_SIZE +
      getTopLeftCoordinatesJS() + // Recalculates top left coordinates
      "var centerX = parseInt(topLeftX + elementWidth/2);" +
      "var centerY = parseInt(topLeftY + elementHeight/2);" +
      "return centerX + ',' + centerY;", elementId);
    throwExceptionIfFailed(result);
    return parseCoordinate(result);
  }
  
  public void click(String elementId) {
    driver.executeScript(
        "function triggerMouseEvent(element, eventType) {" +
        "var event = element.ownerDocument.createEvent('MouseEvents');" +
        "var view = element.ownerDocument.defaultView;" +
        "event.initMouseEvent(eventType, true, true, view, 1, 0, 0, 0, 0," +
        "    false, false, false, false, 0, element);" +
        "element.dispatchEvent(event);" +
        "}" +
        "var doc = document.documentElement;" +
        "if (arguments[0] in doc.androiddriver_elements) {" +
        "  var element = doc.androiddriver_elements[arguments[0]];" +
        "  triggerMouseEvent(element, 'mouseover');" +
        "  triggerMouseEvent(element, 'mousemove');" +
        "  triggerMouseEvent(element, 'mousedown');" +
        "  document.title = 'checking focus';" +
        "  if (element.ownerDocument.activeElement != element) {" +
        "    if (element.ownerDocument.activeElement) {" +
        "      element.ownerDocument.activeElement.blur();" +
        "    }" +
        "    element.focus();" +
        "  }" +
        "  triggerMouseEvent(element, 'mouseup');" +
        "  triggerMouseEvent(element, 'click');" +
        "}",
        elementId);
  }
  
  public void submit(String elementId) {
    driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
        "  var doc = document.documentElement;" +
        "  if (arguments[0] in doc.androiddriver_elements) {" +
        "    var current = doc.androiddriver_elements[arguments[0]];" +
        "    while (current && current != element.ownerDocument.body) {" +
        "      if (current.tagName.toLowerCase() == 'form') {" +
        "        var e = current.ownerDocument.createEvent('HTMLEvents');" +
        "        e.initEvent('submit', true, true);" +
        "        if (current.dispatchEvent(e)) {" +
        "          current.submit();" +
        "        }" +
        "        return;" +
        "      }" +
        "      current = current.parentNode;" +
        "    }" +
        "  }" +
        "}",
        elementId);
  }
  
  public void setSelected(String elementId) {
    String result = (String)driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == true) {" +
        "  return '" + STALE + "';" +
        "}" +
        isDisplayedJs() +
        "if (isDisplayed == false) {" +
        "  return '" + NOT_VISIBLE + "';" +
        "} else if (element.disabled == true) {" +
        "return '" + DISABLED + "';" +
        "}" +
        "var changed = false;" +
        "if (element.tagName.toLowerCase() == 'option') {" +
        "  if (!element.selected) {" +
        "    element.selected = changed = true;" +
        "  }" +
        "} else if (element.tagName.toLowerCase() == 'input') {" +
        "  if (!element.checked) {" +
        "    element.checked = changed = true;" +
        "  }" +
        "} else {" +
        "  return '" + UNSELECTABLE + "'" +
        "}" +
        "if (changed) {" +
        "  var event = element.ownerDocument.createEvent('HTMLEvents');" +
        "  event.initEvent('change', true, true);" +
        "  element.dispatchEvent(event);" +
        "}" +
        "return 'true';",
        elementId);
    throwExceptionIfFailed(result);
  }
  
  public boolean isSelected(String elementId) {
    Object result = driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
          IS_SELECTED +
          "return isSelected;" +
        "}" +
        "return '" + STALE + "'",
        elementId);
    throwExceptionIfFailed(String.valueOf(result));

    return getBoolean(result);
  }

  public boolean toggle(String elementId) {
    Object result = driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
          isDisplayedJs() +
        "  if (isDisplayed == false) {" +
        "    return '" + NOT_VISIBLE + "';" +
        "  }" +
        "  element.focus();" +
        "  if ((element.type && element.type.toLowerCase() == 'radio')" +
            " || element.tagName.toLowerCase() == 'option') {" +
        "    return '" + UNSUPPORTED + "';" +
        "  }" +
        "  if (element instanceof HTMLOptionElement){" +
        "    return element.selected = !element.selected;" +
        "  }else {" +
        "    return element.checked = !element.checked;" +
        "  } " +
        "}" +
        "return '" + STALE + "'",
        elementId);
    throwExceptionIfFailed(String.valueOf(result));
    return (Boolean) result;
  }
  
  public boolean isDisplayed(String elementId) {
    Object result = driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
          isDisplayedJs() +
          "return isDisplayed;" +
        "}" +
        "return '" + STALE + "';",
        elementId);
    throwExceptionIfFailed(String.valueOf(result));
    return getBoolean(result);
  }

  public String getValueOfCssProperty(String using, boolean computedStyle, String elementId) {
    String result = (String) driver.executeScript(
        initCacheJs() +
        isElementStaleJs() +
        "if (isStale == false) {" +
        (computedStyle ?
        ("  if (element.currentStyle) " +
         "    return element.currentStyle[arguments[1]]; " +
         "  else if (window.getComputedStyle) " +
         "    return window.document.defaultView.getComputedStyle(element, null)" +
                  ".getPropertyValue(arguments[1]); ")
         :
         "  return element.style." + using + ";") +
         "}" +
         "return '" + STALE + "';",
        elementId, using);
    throwExceptionIfFailed(result);
    return result;
  }

  /**
   * JavaScript code to be injected in the webview to initialise the cache for
   * the window in whose context the script will execute.
   */
  public static String initCacheJs() {
    return
        "var doc = document.documentElement;" +
        "if (!doc.androiddriver_elements) {" +
        "  doc.androiddriver_elements = {};" +
        "  doc.androiddriver_next_id = 0;" +
        "  doc.androiddriver_elements[0] = document;" +
        "}";
  }

  /**
   * Returns  the Javascript to install XPath in the webview.
   */
  private String installXPathJs() {
    // The XPath library is installed in the main context. For frames, the
    // main context installs the XPath library in the frame context.
    // First it sets the default for the xpath library and expose the installer,
    // then it includes the actual xpath library. It calls
    // window.install() to install it.
    return
        "if (!document.evaluate) {" +
        "  var currentWindow = window;" +
        // We need to run the installation code in the main context
        "  with (window.top) {" +
        "  try {" +
        "    var body = document.getElementsByTagName('body')[0];" +
        "    if (body == undefined) {" +
        "      body = document.createElement('body');" +
        "      document.getElementsByTagName('html')[0].appendChild(body);" +
        "    }" +
        "    var install_tag = document.createElement('script'); " +
        "    install_tag.type = 'text/javascript'; " +
        "    install_tag.innerHTML= 'window.jsxpath = { exportInstaller : true };'; " +
        "    body.appendChild(install_tag);" +
        "    var load_tag = document.createElement('script'); " +
        "    load_tag.type = 'text/javascript'; " +
        "    load_tag.src = 'http://localhost:8080/resources/js'; " +
        "    body.appendChild(load_tag);" +
        "    if (!window.install) {" +
        "      return '" + FAILED + "_window.install undefined!'" +
        "    }" +
        "    window.install(currentWindow);" +
        "    if (!currentWindow.document.evaluate) {" +
        "      return '" + FAILED + "_document.evaluate undefined!'};" +
        "  } catch (error) {" +
        "    return '" + FAILED + "_' + error;" +
        "  }" +
        "  }" +  // with(window.top)
        "}";
  }

  /**
   * Return the Javascript to determine weather an element is stale.
   */
  private String isElementStaleJs() {
    return
        "var isStale = false;" +
        "var doc = document.documentElement;" +
        "if (arguments[0] in doc.androiddriver_elements) {" +
        "  var element = doc.androiddriver_elements[arguments[0]];" +
        "  if (!element) {" +
        "    isStale = true;" + 
        "  } else {" +
        "    var aParent = element;" +
        "    while (aParent && aParent != doc) {" +
        "      aParent = aParent.parentNode;" +
        "    }" +
        "    if (aParent !== doc) {" +
        "      delete doc.androiddriver_elements[arguments[0]];" +
        "      isStale = true;" +
        "    } else {" +
        "      isStale = false;" +
        "    }" +
        "  }" +
        "} else { " +
        "    isStale = true;" +
        "};";
  }

  private String getTopLeftCoordinatesJS() {
    return "var topLeftX = 0;" +
        "var topLeftY = 0; "+
        "if (element.getBoundingClientRect) {" +
        "  topLeftX = element.getBoundingClientRect().left;" +
        "  topLeftY = element.getBoundingClientRect().top;" +
        "}";
  }
  
  private static String isDisplayedJs() {
    return 
        "var isDisplayed = true;" +
        "var body = document.body; " +
        "var aParent = element;" +
        "while(aParent && aParent!= body) {" +
        "  if((aParent.style && (aParent.style.display == 'none'" +
            "|| aParent.style.visibility == 'hidden'))" +
            "|| (element.type && (element.type == 'hidden'))) {" +
        "    isDisplayed = false;  " +
        "  }" +
        "  aParent = aParent.parentNode; " +
        "}";
  }

  private Point parseCoordinate(String result) {
    String[] coordinates = result.split(",");
    try {
      if (coordinates.length == 2) {
        return new Point(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));
      }
      else {
        throw new WebDriverException("Cannot parse coordinates: " + result);
      }
    } catch (Exception e) {
      throw new WebDriverException("Failed to parse: " + result, e);
    }
  }

  private List<WebElement> createWebElementsWithIds(List<Integer> ids, String elementId) {
    List<WebElement> elements = new ArrayList<WebElement>();
    // Return empty list when there are no children of a node
    if (ids.size() == 0) {
      return elements;
    }
    try {
      for (int index : ids) {
        if (index > 0) {
          elements.add(new AndroidWebElement(driver, String.valueOf(index)));
        }
      }
      return elements;
    } catch (NumberFormatException e) {
      throw new InternalError("Javascript injection failed. Got result: " + ids);
    } catch (ClassCastException e) {
      Logger.log(Log.DEBUG, "WD", "ID: " + ids.get(0));
      throw e;
    }
  }

  private List<Object> executeAndRetry(String using, String elementId, String toExecute) {
    List<Object> result = new ArrayList<Object>();
    for (int i = 0; i < MAX_XPATH_ATTEMPTS; i++) {
      Object scriptResult = driver.executeScript(toExecute, using, elementId);
      Logger.log(Log.DEBUG, "WD", "GOT JAVASCRIPT RESULT: " + scriptResult);
      if (scriptResult instanceof String && ((String) scriptResult).startsWith(FAILED)) {
        try {
          Logger.log(Log.DEBUG, LOG_TAG, "executeAndRetry Script: " + toExecute);
          Thread.sleep(XPATH_RETRY_TIMEOUT);
        } catch (InterruptedException e) {
          Logger.log(Log.ERROR, LOG_TAG, "executeAndRetry InterruptedException: " + e.getMessage());
          break;
        }
      } else {
        if (scriptResult instanceof List) {
          result = (List) scriptResult;
          break;
        } else {
          Logger.log(Log.DEBUG, LOG_TAG, "Not expected type " + scriptResult);
        }
      }
    }
    return result;
  }
  
  private void throwExceptionIfFailed(String result) {
    if (UNSELECTABLE.equals(result)) {
      throw new UnsupportedOperationException("Element is not selectable.");
    } else if (STALE.equals(result)) {
      throw new StaleElementReferenceException("Element is stale.");
    } else if (NOT_VISIBLE.equals(result)) {
      throw new ElementNotVisibleException("Element not visible.");
    } else if (DISABLED.equals(result)) {
      throw new UnsupportedOperationException("Cannot select disabled element.");
    } else if (UNSUPPORTED.equals(result)) {
      throw new UnsupportedOperationException("Cannot toggle a radio button.");
    }
  }
  
  private WebElement getFirstElement(List<WebElement> elements) {
    if (elements.size() > 0) {
      return elements.get(0);
    }
    throw new NoSuchElementException("Element not found.");
  }
  
  private Boolean getBoolean(Object result) {
    if (result instanceof Boolean) {
      return (Boolean) result;
    }
    throw new WebDriverException("Unknown result type " + result);
  }
}
