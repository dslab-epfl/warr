/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.
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

package org.openqa.selenium.htmlunit;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.TopLevelWindow;
import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.WebWindowEvent;
import com.gargoylesoftware.htmlunit.WebWindowListener;
import com.gargoylesoftware.htmlunit.WebWindowNotFoundException;
import com.gargoylesoftware.htmlunit.html.BaseFrame;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLCollection;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLElement;

import net.sourceforge.htmlunit.corejs.javascript.*;

import org.openqa.selenium.*;
import org.openqa.selenium.browserlaunchers.Proxies;
import org.openqa.selenium.interactions.ActionChainsGenerator;
import org.openqa.selenium.interactions.DefaultActionChainsGenerator;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByLinkText;
import org.openqa.selenium.internal.FindsByName;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.internal.FindsByXPath;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.remote.CapabilityType.SUPPORTS_FINDING_BY_CSS;

public class HtmlUnitDriver implements WebDriver, SearchContext, JavascriptExecutor,
    FindsById, FindsByLinkText, FindsByXPath, FindsByName, FindsByCssSelector,
    FindsByTagName, HasCapabilities, HasInputDevices {

  private WebClient webClient;
  private WebWindow currentWindow;

  private boolean enableJavascript;
  private ProxyConfig proxyConfig;
  private final BrowserVersion version;
  private long implicitWait = 0;
  private long scriptTimeout = 0;
  private HtmlUnitKeyboard keyboard;
  private HtmlUnitMouse mouse;
  private boolean gotPage;

  public HtmlUnitDriver(BrowserVersion version) {
    this.version = version;
    webClient = createWebClient(version);
    currentWindow = webClient.getCurrentWindow();

    webClient.addWebWindowListener(new WebWindowListener() {
      public void webWindowOpened(WebWindowEvent webWindowEvent) {
        // Ignore
      }

      public void webWindowContentChanged(WebWindowEvent event) {
        if (event.getWebWindow() != currentWindow) {
          return;
        }

        // Do we need to pick some new default content?
        switchToDefaultContentOfWindow(currentWindow);
      }

      public void webWindowClosed(WebWindowEvent event) {
        // Check if the event window refers to us or one of our parent windows
        // setup the currentWindow appropriately if necessary
        WebWindow curr = currentWindow;
        do {
          // Instance equality is okay in this case
          if (curr == event.getWebWindow()) {
            currentWindow = currentWindow.getTopWindow();
            return;
          }
          curr = curr.getParentWindow();
        } while (curr != currentWindow.getTopWindow());
      }
    });

    // Now put us on the home page, like a real browser
    get(webClient.getHomePage());
    gotPage = false;
    resetKeyboardAndMouseState();
  }

  public HtmlUnitDriver() {
    this(false);
  }

  public HtmlUnitDriver(boolean enableJavascript) {
    this(BrowserVersion.getDefault());
    setJavascriptEnabled(enableJavascript);
  }

  /**
   * Note: There are two configuration modes for the HtmlUnitDriver using this
   * constructor. The first is where the browserName is "firefox",
   * "internet explorer" and browserVersion denotes the desired version.
   * The second one is where the browserName is "htmlunit" and the
   * browserVersion denotes the required browser AND its version. In this
   * mode the browserVersion could either be "firefox" for Firefox or
   * "internet explorer-7" for IE 7. The Remote WebDriver uses the second mode
   * - the first mode is deprecated and should not be used.
   */
  public HtmlUnitDriver(Capabilities capabilities) {
    this(determineBrowserVersion(capabilities));

    setJavascriptEnabled(capabilities.isJavascriptEnabled());

    if (capabilities.getCapability(CapabilityType.PROXY) != null) {
      Proxy proxy = Proxies.extractProxy(capabilities);
      String fullProxy = proxy.getHttpProxy();
      if (fullProxy != null) {
        int index = fullProxy.indexOf(":");
        if (index != -1) {
          String host = fullProxy.substring(0, index);
          int port = Integer.parseInt(fullProxy.substring(index + 1));
          setProxy(host, port);
        } else {
          setProxy(fullProxy, 0);
        }
      }
    }
  }

  // Package visibility for testing
  static BrowserVersion determineBrowserVersion(Capabilities capabilities) {
    String browserName = null;
    String browserVersion = null;

    String[] splitVersion = capabilities.getVersion().split("-");
    if (splitVersion.length > 1) {
      browserVersion = splitVersion[1];
      browserName = splitVersion[0];
    } else {
      browserName = capabilities.getVersion();
      browserVersion = "";
    }

    // This is for backwards compatibility - in case there are users who are trying to
    // configure the HtmlUnitDriver by using the c'tor with capabilities.
    if (! capabilities.getBrowserName().equals("htmlunit")) {
      browserName = capabilities.getBrowserName();
      browserVersion = capabilities.getVersion();
    }

    if ("firefox".equals(browserName)) {
      return BrowserVersion.FIREFOX_3;
    }
    
    if ("internet explorer".equals(browserName)) {
      // Try and convert the version
      try {
        int version = Integer.parseInt(browserVersion);
        switch (version) {
          case 6:
            return BrowserVersion.INTERNET_EXPLORER_6;
          case 7:
            return BrowserVersion.INTERNET_EXPLORER_7;
          case 8:
            return BrowserVersion.INTERNET_EXPLORER_8;
          default:
            return BrowserVersion.INTERNET_EXPLORER_8;
        }
      } catch (NumberFormatException e) {
        return BrowserVersion.INTERNET_EXPLORER_8;
      }
    }

    return BrowserVersion.getDefault();
  }

  private WebClient createWebClient(BrowserVersion version) {
    WebClient client = newWebClient(version);
    client.setHomePage(WebClient.URL_ABOUT_BLANK.toString());
    client.setThrowExceptionOnFailingStatusCode(false);
    client.setPrintContentOnFailingStatusCode(false);
    client.setJavaScriptEnabled(enableJavascript);
    client.setRedirectEnabled(true);
    client.setRefreshHandler(new WaitingRefreshHandler());

    try {
      client.setUseInsecureSSL(true);
    } catch (GeneralSecurityException e) {
      throw new WebDriverException(e);
    }

    // Ensure that we've set the proxy if necessary
    if (proxyConfig != null) {
      client.setProxyConfig(proxyConfig);
    }

    return modifyWebClient(client);
  }

  /**
   * Create the underlying webclient, but don't set any fields on it.
   *
   * @param version Which browser to emulate
   * @return a new instance of WebClient.
   */
  protected WebClient newWebClient(BrowserVersion version) {
    return new WebClient(version);
  }

  /**
   * Child classes can override this method to customise the webclient that the HtmlUnit driver
   * uses.
   *
   * @param client The client to modify
   * @return The modified client
   */
  protected WebClient modifyWebClient(WebClient client) {
    // Does nothing here to be overridden.
    return client;
  }

  public void setProxy(String host, int port) {
    proxyConfig = new ProxyConfig(host, port);
    webClient.setProxyConfig(proxyConfig);
  }

  public void setAutoProxy(String autoProxyUrl) {
    proxyConfig = new ProxyConfig();
    proxyConfig.setProxyAutoConfigUrl(autoProxyUrl);
    webClient.setProxyConfig(proxyConfig);
  }

  public Capabilities getCapabilities() {
    DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();

    capabilities.setPlatform(Platform.getCurrent());
    capabilities.setJavascriptEnabled(isJavascriptEnabled());
    capabilities.setCapability(SUPPORTS_FINDING_BY_CSS, true);

    return capabilities;
  }

  public void get(String url) {
    // Prevent the malformed url exception.
    if (WebClient.URL_ABOUT_BLANK.toString().equals(url)) {
      get(WebClient.URL_ABOUT_BLANK);
      return;
    }

    URL fullUrl;
    try {
      fullUrl = new URL(url);
    } catch (Exception e) {
      throw new WebDriverException(e);
    }

    get(fullUrl);
  }

  /**
   * Allows HtmlUnit's about:blank to be loaded in the constructor, and may be useful for other
   * tests?
   *
   * @param fullUrl The URL to visit
   */
  protected void get(URL fullUrl) {
    try {
      // A "get" works over the entire page
      currentWindow = currentWindow.getTopWindow();
      webClient.getPage(fullUrl);
    } catch (UnknownHostException e) {
      // This should be fine
    } catch (ConnectException e) {
      // This might be expected
    } catch (Exception e) {
      throw new WebDriverException(e);
    }

    gotPage = true;
    pickWindow();
    resetKeyboardAndMouseState();
  }

  private void resetKeyboardAndMouseState() {
    keyboard = new HtmlUnitKeyboard(this);
    mouse = new HtmlUnitMouse(this, keyboard);  
  }

  protected void pickWindow() {
    // TODO(simon): HtmlUnit tries to track the current window as the frontmost. We don't
    if (currentWindow == null) {
      currentWindow = webClient.getCurrentWindow();
    }
  }

  public String getCurrentUrl() {
    // TODO(simon): Blech. I can see this being baaad
    URL url = getRawUrl();
    if (url == null) {
      return null;
    }

    return url.toString();
  }

  public String getTitle() {
    Page page = lastPage();
    if (page == null || !(page instanceof HtmlPage)) {
      return null; // no page so there is no title
    }
    if (currentWindow instanceof FrameWindow) {
      page = ((FrameWindow) currentWindow).getTopWindow().getEnclosedPage();
    }
    
    return ((HtmlPage) page).getTitleText();
  }

  public WebElement findElement(By by) {
    return findElement(by, this);
  }

  public List<WebElement> findElements(By by) {
    return findElements(by, this);
  }

  public String getPageSource() {
    Page page = lastPage();
    if (page == null) {
      return null;
    }

    if (page instanceof SgmlPage) {
      return ((SgmlPage) page).asXml();
    }
    WebResponse response = page.getWebResponse();
    return response.getContentAsString();
  }

  public void close() {
    if (currentWindow != null) {
      ((TopLevelWindow) currentWindow.getTopWindow()).close();
    }

    webClient = createWebClient(version);
  }

  public void quit() {
    if (webClient != null) {
      webClient.closeAllWindows();
      webClient = null;
    }
    currentWindow = null;
  }

  public Set<String> getWindowHandles() {
    final Set<String> allHandles = new HashSet<String>();
    for (final WebWindow window : webClient.getTopLevelWindows()) {
      allHandles.add(String.valueOf(System.identityHashCode(window)));
    }

    return allHandles;
  }

  public String getWindowHandle() {
    return String.valueOf(System.identityHashCode(currentWindow.getTopWindow()));
  }

  public Object executeScript(String script, final Object... args) {
    HtmlPage page = getPageToInjectScriptInto();

    script = "function() {" + script + "\n};";
    ScriptResult result = page.executeJavaScript(script);
    Function func = (Function) result.getJavaScriptResult();

    final Scriptable scope = (Scriptable) page.getEnclosingWindow().getScriptObject();

    final Object[] parameters = new Object[args.length];
    final ContextAction action = new ContextAction() {
      public Object run(final Context context) {
        for (int i = 0; i < args.length; i++) {
          parameters[i] = parseArgumentIntoJavsacriptParameter(context, scope, args[i]);
        }
        return null;
      }
    };
    webClient.getJavaScriptEngine().getContextFactory().call(action);

    result = page.executeJavaScriptFunctionIfPossible(
        func,
        (ScriptableObject) currentWindow.getScriptObject(),
        parameters,
        page.getDocumentElement());

    return parseNativeJavascriptResult(result);
  }

  public Object executeAsyncScript(String script, Object... args) {
    HtmlPage page = getPageToInjectScriptInto();

    Object result = new AsyncScriptExecutor(page, scriptTimeout)
        .execute(script, args);

    return parseNativeJavascriptResult(result);
  }

  private HtmlPage getPageToInjectScriptInto() {
    if (!isJavascriptEnabled()) {
      throw new UnsupportedOperationException(
          "Javascript is not enabled for this HtmlUnitDriver instance");
    }

    final Page lastPage = lastPage();
    if (!(lastPage instanceof HtmlPage)) {
      throw new UnsupportedOperationException("Cannot execute JS against a plain text page");
    } else if (!gotPage) {
      // just to make ExecutingJavascriptTest.testShouldThrowExceptionIfExecutingOnNoPage happy
      // but does this limitation make sense?
      throw new WebDriverException("Can't execute JavaScript before a page has been loaded!");
    }

    return (HtmlPage) lastPage;
  }

  private Object parseArgumentIntoJavsacriptParameter(
      Context context, Scriptable scope, Object arg) {
    if (!(arg instanceof HtmlUnitWebElement ||
          arg instanceof HtmlElement || // special case the underlying type
          arg instanceof Number ||
          arg instanceof String ||
          arg instanceof Boolean ||
          arg.getClass().isArray() ||
          arg instanceof Collection<?>)) {
      throw new IllegalArgumentException(
          "Argument must be a string, number, boolean or WebElement: " +
          arg + " (" + arg.getClass() + ")");
    }

    if (arg instanceof HtmlUnitWebElement) {
      HtmlElement element = ((HtmlUnitWebElement) arg).getElement();
      return element.getScriptObject();
    } else if (arg instanceof HtmlElement) {
      return ((HtmlElement) arg).getScriptObject();
    } else if (arg instanceof Collection<?>) {
      List<Object> list = new ArrayList<Object>();
      for (Object o : (Collection<?>) arg) {
        list.add(parseArgumentIntoJavsacriptParameter(context, scope, o));
      }
      return context.newArray(scope, list.toArray());
    } else {
      return arg;
    }
  }

  public Keyboard getKeyboard() {
    return keyboard;
  }

  public Mouse getMouse() {
    return mouse;
  }

  public ActionChainsGenerator actionsBuilder() {
    return new DefaultActionChainsGenerator(this);
  }

  protected interface JavaScriptResultsCollection {
    int getLength();

    Object item(int index);
  }

  private Object parseNativeJavascriptResult(Object result) {
    Object value;
    if (result instanceof ScriptResult) {
      value = ((ScriptResult) result).getJavaScriptResult();
    } else {
      value = result;
    }
    if (value instanceof HTMLElement) {
      return newHtmlUnitWebElement(((HTMLElement) value).getDomNodeOrDie());
    }

    if (value instanceof Number) {
      final Number n = (Number) value;
      final String s = n.toString();
      if (s.indexOf(".") == -1 || s.endsWith(".0")) { // how safe it is? enough for the unit tests!
    	  return n.longValue();
      }
      return n.doubleValue();
    }

    if (value instanceof NativeObject) {
    	final Map<String, Object> map = new HashMap<String, Object>((NativeObject) value);
    	for (final Entry<String, Object> e : map.entrySet()) {
    		e.setValue(parseNativeJavascriptResult(e.getValue()));
    	}
    	return map;
    }
    
    if (value instanceof NativeArray) {
      final NativeArray array = (NativeArray) value;

      JavaScriptResultsCollection collection = new JavaScriptResultsCollection() {
        public int getLength() {
          return (int) array.getLength();
        }

        public Object item(int index) {
          return array.get(index);
        }
      };

      return parseJavascriptResultsList(collection);
    }

    if (value instanceof HTMLCollection) {
      final HTMLCollection array = (HTMLCollection) value;

      JavaScriptResultsCollection collection = new JavaScriptResultsCollection() {
        public int getLength() {
          return array.getLength();
        }

        public Object item(int index) {
          return array.get(index);
        }
      };

      return parseJavascriptResultsList(collection);
    }

    if (value instanceof Undefined) {
      return null;
    }

    return value;
  }

  private List<Object> parseJavascriptResultsList(JavaScriptResultsCollection array) {
    List<Object> list = new ArrayList<Object>(array.getLength());
    for (int i = 0; i < array.getLength(); ++i) {
      list.add(parseNativeJavascriptResult(array.item(i)));
    }
    return list;
  }

  public TargetLocator switchTo() {
    return new HtmlUnitTargetLocator();
  }

  private void switchToDefaultContentOfWindow(WebWindow window) {
    Page page = window.getEnclosedPage();
    if (page instanceof HtmlPage) {
      currentWindow = window;
    }
  }

  public Navigation navigate() {
    return new HtmlUnitNavigation();
  }

  protected Page lastPage() {
    return currentWindow.getEnclosedPage();
  }

  public WebElement findElementByLinkText(String selector) {
    if (!(lastPage() instanceof HtmlPage)) {
      throw new IllegalStateException("Cannot find links for " + lastPage());
    }

    String expectedText = selector.trim();

    List<HtmlAnchor> anchors = ((HtmlPage) lastPage()).getAnchors();
    for (HtmlAnchor anchor : anchors) {
      if (expectedText.equals(anchor.asText().trim())) {
        return newHtmlUnitWebElement(anchor);
      }
    }
    throw new NoSuchElementException("No link found with text: " + expectedText);
  }

  protected WebElement newHtmlUnitWebElement(HtmlElement element) {
    return new HtmlUnitWebElement(this, element);
  }

  public List<WebElement> findElementsByLinkText(String selector) {
    List<WebElement> elements = new ArrayList<WebElement>();

    if (!(lastPage() instanceof HtmlPage)) {
      return elements;
    }

    String expectedText = selector.trim();

    List<HtmlAnchor> anchors = ((HtmlPage) lastPage()).getAnchors();
    for (HtmlAnchor anchor : anchors) {
      if (expectedText.equals(anchor.asText().trim())) {
        elements.add(newHtmlUnitWebElement(anchor));
      }
    }
    return elements;
  }

  public WebElement findElementById(String id) {
    if (!(lastPage() instanceof HtmlPage)) {
      throw new NoSuchElementException("Unable to locate element by id for " + lastPage());
    }

    try {
      HtmlElement element = ((HtmlPage) lastPage()).getHtmlElementById(id);
      return newHtmlUnitWebElement(element);
    } catch (ElementNotFoundException e) {
      throw new NoSuchElementException("Unable to locate element with ID: " + id);
    }
  }

  public List<WebElement> findElementsById(String id) {
    return findElementsByXPath("//*[@id='" + id + "']");
  }

  public WebElement findElementByCssSelector(String using) {
    if (!(lastPage() instanceof HtmlPage)) {
      throw new NoSuchElementException("Unable to locate element using css: " + lastPage());
    }

    DomNode node = ((HtmlPage) lastPage()).querySelector(using);

    if (node instanceof HtmlElement) {
      return newHtmlUnitWebElement((HtmlElement) node);
    }

    throw new NoSuchElementException("Returned node was not an HTML element");
  }

  public List<WebElement> findElementsByCssSelector(String using) {
        if (!(lastPage() instanceof HtmlPage)) {
      throw new NoSuchElementException("Unable to locate element using css: " + lastPage());
    }

    DomNodeList<DomNode> allNodes = ((HtmlPage) lastPage()).querySelectorAll(using);

    List<WebElement> toReturn = new ArrayList<WebElement>();

    for (DomNode node : allNodes) {
      if (node instanceof HtmlElement) {
        toReturn.add(newHtmlUnitWebElement((HtmlElement) node));
      } else {
        throw new NoSuchElementException("Returned node was not an HTML element");
      }
    }

    return toReturn;
  }

  public WebElement findElementByName(String name) {
    if (!(lastPage() instanceof HtmlPage)) {
      throw new IllegalStateException("Unable to locate element by name for " + lastPage());
    }

    List<HtmlElement> allElements = ((HtmlPage) lastPage()).getElementsByName(name);
    if (allElements.size() > 0) {
      return newHtmlUnitWebElement(allElements.get(0));
    }

    throw new NoSuchElementException("Unable to locate element with name: " + name);
  }

  public List<WebElement> findElementsByName(String using) {
    if (!(lastPage() instanceof HtmlPage)) {
      return new ArrayList<WebElement>();
    }

    List<HtmlElement> allElements = ((HtmlPage) lastPage()).getElementsByName(using);
    return convertRawHtmlElementsToWebElements(allElements);
  }

  public WebElement findElementByTagName(String name) {
    if (!(lastPage() instanceof HtmlPage)) {
      throw new IllegalStateException("Unable to locate element by name for " + lastPage());
    }

    NodeList allElements = ((HtmlPage) lastPage()).getElementsByTagName(name);
    if (allElements.getLength() > 0) {
      return newHtmlUnitWebElement((HtmlElement) allElements.item(0));
    }

    throw new NoSuchElementException("Unable to locate element with name: " + name);
  }

  public List<WebElement> findElementsByTagName(String using) {
    if (!(lastPage() instanceof HtmlPage)) {
      return new ArrayList<WebElement>();
    }

    NodeList allElements = ((HtmlPage) lastPage()).getElementsByTagName(using);
    List<WebElement> toReturn = new ArrayList<WebElement>(allElements.getLength());
    for (int i = 0; i < allElements.getLength(); i++) {
      Node item = allElements.item(i);
      if (item instanceof HtmlElement) {
        toReturn.add(newHtmlUnitWebElement((HtmlElement) item));
      }
    }
    return toReturn;
  }

  public WebElement findElementByXPath(String selector) {
    if (!(lastPage() instanceof HtmlPage)) {
      throw new IllegalStateException("Unable to locate element by xpath for " + lastPage());
    }

    Object node = ((HtmlPage) lastPage()).getFirstByXPath(selector);
    if (node == null) {
      throw new NoSuchElementException("Unable to locate a node using " + selector);
    }
    if (node instanceof HtmlElement) {
      return newHtmlUnitWebElement((HtmlElement) node);
    }
    throw new NoSuchElementException(String.format("Unable to locate element with xpath %s", selector));
  }

  public List<WebElement> findElementsByXPath(String selector) {
    if (!(lastPage() instanceof HtmlPage)) {
      return new ArrayList<WebElement>();
    }

    List<?> nodes = ((HtmlPage) lastPage()).getByXPath(selector);
    return convertRawHtmlElementsToWebElements(nodes);
  }

  private List<WebElement> convertRawHtmlElementsToWebElements(List<?> nodes) {
    List<WebElement> elements = new ArrayList<WebElement>();

    for (Object node : nodes) {
      if (node instanceof HtmlElement) {
        elements.add(newHtmlUnitWebElement((HtmlElement) node));
      }
    }

    return elements;
  }

  public boolean isJavascriptEnabled() {
    return webClient.isJavaScriptEnabled();
  }

  public void setJavascriptEnabled(boolean enableJavascript) {
    this.enableJavascript = enableJavascript;
    webClient.setJavaScriptEnabled(enableJavascript);
  }

  private class HtmlUnitTargetLocator implements TargetLocator {

    public WebDriver frame(int index) {
      HtmlPage currentPage = (HtmlPage) currentWindow.getEnclosedPage();
      try {
        // 1.) try to find frame in current window ...
        currentWindow = currentPage.getFrames().get(index);
      } catch (IndexOutOfBoundsException ignored) {
        throw new NoSuchFrameException("Cannot find frame: " + index);
      }
      return HtmlUnitDriver.this;
    }

    public WebDriver frame(final String nameOrId) {
      // First check for a frame with the matching name.
      HtmlPage startPage = (HtmlPage) currentWindow.getEnclosedPage();
      for (final FrameWindow frameWindow : startPage.getFrames()) {
        if (frameWindow.getName().equals(nameOrId)) {
          currentWindow = frameWindow;
          return HtmlUnitDriver.this;
        }
      }

      // Next, check fora  frame with a matching ID.  For simplicity, assume the ID is unique.
      // Users can still switch to frames with non-unique IDs using a WebElement switch:
      //   WebElement frameElement = driver.findElement(By.xpath("//frame[@id=\"foo\"]"));
      //   driver.switchTo().frame(frameElement);
      try {
        HtmlUnitWebElement element =
            (HtmlUnitWebElement) HtmlUnitDriver.this.findElementById(nameOrId);
        HtmlElement domElement = element.getElement();
        if (domElement instanceof BaseFrame) {
          currentWindow = ((BaseFrame) domElement).getEnclosedWindow();
          return HtmlUnitDriver.this;
        }
      } catch (NoSuchElementException ignored) {
      }

      throw new NoSuchFrameException("Unable to locate frame with name or ID: " + nameOrId);
    }

    public WebDriver frame(WebElement frameElement) {
      while (frameElement instanceof WrapsElement) {
        frameElement = ((WrapsElement) frameElement).getWrappedElement();
      }

      HtmlUnitWebElement webElement = (HtmlUnitWebElement) frameElement;
      webElement.assertElementNotStale();

      HtmlElement domElement = webElement.getElement();
      if (!(domElement instanceof BaseFrame)) {
        throw new NoSuchFrameException(webElement.getTagName() + " is not a frame element.");
      }

      currentWindow = ((BaseFrame) domElement).getEnclosedWindow();
      return HtmlUnitDriver.this;
    }

    private WebWindow getWindowByNumericFrameId(String currentFrameId, HtmlPage page) {
      try {
        final int index = Integer.parseInt(currentFrameId);
        return page.getFrames().get(index);
      }
      catch (final NumberFormatException e) {
        // nothing - fall through to returning null.
      }
      catch (final IndexOutOfBoundsException e) { // frames may have an int as name
        // nothing - fall through to returning null.
      }

      return null;
    }

    public WebDriver window(String windowId) {
      try {
        WebWindow window = webClient.getWebWindowByName(windowId);
        return finishSelecting(window);
      } catch (WebWindowNotFoundException e) {

        List<WebWindow> allWindows = webClient.getWebWindows();
        for (WebWindow current : allWindows) {
          WebWindow top = current.getTopWindow();
          if (String.valueOf(System.identityHashCode(top)).equals(windowId)) {
            return finishSelecting(top);
          }
        }
        throw new NoSuchWindowException("Cannot find window: " + windowId);
      }
    }

    private WebDriver finishSelecting(WebWindow window) {
      webClient.setCurrentWindow(window);
      currentWindow = window;
      pickWindow();
      return HtmlUnitDriver.this;
    }

    public WebDriver defaultContent() {
      switchToDefaultContentOfWindow(currentWindow.getTopWindow());
      return HtmlUnitDriver.this;
    }

    public WebElement activeElement() {
      Page page = currentWindow.getEnclosedPage();
      if (page instanceof HtmlPage) {
        HtmlElement element = ((HtmlPage) page).getFocusedElement();
        if (element == null) {
          List<? extends HtmlElement> allBodies =
              ((HtmlPage) page).getDocumentElement().getHtmlElementsByTagName("body");
          if (allBodies.size() > 0) {
            return newHtmlUnitWebElement(allBodies.get(0));
          }
        } else {
          return newHtmlUnitWebElement(element);
        }
      }

      throw new NoSuchElementException("Unable to locate element with focus or body tag");
    }

    public Alert alert() {
      throw new UnsupportedOperationException("alert()");
    }
  }

  protected <X> X implicitlyWaitFor(Callable<X> condition) {
    long end = System.currentTimeMillis() + implicitWait;
    Exception lastException = null;

    do {
      X toReturn = null;
      try {
        toReturn = condition.call();
      } catch (Exception e) {
        lastException = e;
      }

      if (toReturn instanceof Boolean && !(Boolean) toReturn) {
        continue;
      }

      if (toReturn != null) {
        return toReturn;
      }

      sleepQuietly(200);
    } while (System.currentTimeMillis() < end);

    if (lastException != null) {
      if (lastException instanceof RuntimeException) {
        throw (RuntimeException) lastException;
      }
      throw new WebDriverException(lastException);
    }

    return null;
  }

  protected WebClient getWebClient() {
    return webClient;
  }

  protected WebWindow getCurrentWindow() {
    return currentWindow;
  }

  private URL getRawUrl() {
    // TODO(simon): I can see this being baaad.
    Page page = lastPage();
    if (page == null) {
      return null;
    }

    return page.getUrl();
  }

  private class HtmlUnitNavigation implements Navigation {

    public void back() {
      try {
        currentWindow.getHistory().back();
      } catch (IOException e) {
        throw new WebDriverException(e);
      }
    }

    public void forward() {
      try {
        currentWindow.getHistory().forward();
      } catch (IOException e) {
        throw new WebDriverException(e);
      }
    }


    public void to(String url) {
      get(url);
    }

    public void to(URL url) {
      get(url);
    }

    public void refresh() {
      if (lastPage() instanceof HtmlPage) {
        try {
          ((HtmlPage) lastPage()).refresh();
        } catch (IOException e) {
          throw new WebDriverException(e);
        }
      }
    }
  }

  public Options manage() {
    return new HtmlUnitOptions();
  }

  private class HtmlUnitOptions implements Options {

    public void addCookie(Cookie cookie) {
      Page page = lastPage();
      if (!(page instanceof HtmlPage)) {
        throw new UnableToSetCookieException("You may not set cookies on a page that is not HTML");
      }

      String domain = getDomainForCookie();
      verifyDomain(cookie, domain);

      webClient.getCookieManager().addCookie(
          new com.gargoylesoftware.htmlunit.util.Cookie(domain, cookie.getName(), cookie.getValue(),
              cookie.getPath(), cookie.getExpiry(), cookie.isSecure()));
    }

    private void verifyDomain(Cookie cookie, String expectedDomain) {
      String domain = cookie.getDomain();
      if (domain == null) {
        return;
      }

      if ("".equals(domain)) {
        throw new InvalidCookieDomainException(
            "Domain must not be an empty string. Consider using null instead");
      }

      // Line-noise-tastic
      if (domain.matches(".*[^:]:\\d+$")) {
        domain = domain.replaceFirst(":\\d+$", "");
      }

      expectedDomain = expectedDomain.startsWith(".") ? expectedDomain : "." + expectedDomain;
      domain = domain.startsWith(".") ? domain : "." + domain;

      if (!expectedDomain.endsWith(domain)) {
        throw new InvalidCookieDomainException(
            String.format(
                "You may only add cookies that would be visible to the current domain: %s => %s",
                domain, expectedDomain));
      }
    }

    public Cookie getCookieNamed(String name) {
      Set<Cookie> allCookies = getCookies();
      for (Cookie cookie : allCookies) {
        if (name.equals(cookie.getName())) {
          return cookie;
        }
      }

      return null;
    }

    public void deleteCookieNamed(String name) {
      CookieManager cookieManager = webClient.getCookieManager();

      URL url = getRawUrl();
      Set<com.gargoylesoftware.htmlunit.util.Cookie> rawCookies =
          webClient.getCookieManager().getCookies(url);
      for (com.gargoylesoftware.htmlunit.util.Cookie cookie : rawCookies) {
        if (name.equals(cookie.getName())) {
          cookieManager.removeCookie(cookie);
        }
      }
    }

    public void deleteCookie(Cookie cookie) {
      deleteCookieNamed(cookie.getName());
    }

    public void deleteAllCookies() {
      webClient.getCookieManager().clearCookies();
    }

    public Set<Cookie> getCookies() {
      URL url = getRawUrl();

      // The about:blank URL (the default in case no navigation took place)
      // does not have a valid 'hostname' part and cannot be used for creating
      // cookies based on it - return an empty set.

      if (!url.toString().startsWith("http")) {
        return new HashSet<Cookie>();
      }

      Set<com.gargoylesoftware.htmlunit.util.Cookie>
          rawCookies =
          webClient.getCookieManager().getCookies(url);

      Set<Cookie> retCookies = new HashSet<Cookie>();
      for (com.gargoylesoftware.htmlunit.util.Cookie c : rawCookies) {
        if (c.getPath() != null && getPath().toLowerCase().startsWith(c.getPath())) {
          retCookies.add(new Cookie.Builder(c.getName(), c.getValue())
              .domain(c.getDomain())
              .path(c.getPath())
              .expiresOn(c.getExpires())
              .isSecure(c.isSecure())
              .build());
        }
      }
      return retCookies;
    }

    private String getPath() {
      return getRawUrl().getPath();
    }

    private String getDomainForCookie() {
      URL current = getRawUrl();
      return current.getHost();
    }

    public Timeouts timeouts() {
      return new HtmlUnitTimeouts();
    }

    public ImeHandler ime() {
      throw new UnsupportedOperationException("Cannot input IME using HtmlUnit.");
    }
  }

  class HtmlUnitTimeouts implements Timeouts {
    public Timeouts implicitlyWait(long time, TimeUnit unit) {
      HtmlUnitDriver.this.implicitWait =
          TimeUnit.MILLISECONDS.convert(Math.max(0, time), unit);
      return this;
    }

    public Timeouts setScriptTimeout(long time, TimeUnit unit) {
      HtmlUnitDriver.this.scriptTimeout = TimeUnit.MILLISECONDS.convert(time, unit);
      return this;
    }
  }

  public WebElement findElementByPartialLinkText(String using) {
    if (!(lastPage() instanceof HtmlPage)) {
      throw new IllegalStateException("Cannot find links for " + lastPage());
    }

    List<HtmlAnchor> anchors = ((HtmlPage) lastPage()).getAnchors();
    for (HtmlAnchor anchor : anchors) {
      if (anchor.asText().contains(using)) {
        return newHtmlUnitWebElement(anchor);
      }
    }
    throw new NoSuchElementException("No link found with text: " + using);
  }

  public List<WebElement> findElementsByPartialLinkText(String using) {

    List<HtmlAnchor> anchors = ((HtmlPage) lastPage()).getAnchors();
    Iterator<HtmlAnchor> allAnchors = anchors.iterator();
    List<WebElement> elements = new ArrayList<WebElement>();
    while (allAnchors.hasNext()) {
      HtmlAnchor anchor = allAnchors.next();
      if (anchor.asText().contains(using)) {
        elements.add(newHtmlUnitWebElement(anchor));
      }
    }
    return elements;
  }

  WebElement findElement(final By locator, final SearchContext context) {
    return implicitlyWaitFor(new Callable<WebElement>() {

      public WebElement call() throws Exception {
          return locator.findElement(context);
      }
    });
  }

  List<WebElement> findElements(final By by, final SearchContext context) {
    long end = System.currentTimeMillis() + implicitWait;
    List<WebElement> found;
    do {
      found = by.findElements(context);
      if (!found.isEmpty()) {
        return found;
      }
    } while (System.currentTimeMillis() < end);

    return found;
  }

  private static void sleepQuietly(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ignored) {
    }
  }
}
