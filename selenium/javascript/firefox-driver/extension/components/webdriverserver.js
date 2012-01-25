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


function WebDriverServer() {
  // We do this here to work around an issue in the import function:
  // https://groups.google.com/group/mozilla.dev.apps.firefox/browse_thread/thread/e178d41afa2ccc87?hl=en&pli=1#
  Components.utils.import('resource://fxdriver/modules/utils.js');

  this.wrappedJSObject = this;
  this.serverSocket =
  Components.classes["@mozilla.org/network/server-socket;1"].
      createInstance(Components.interfaces.nsIServerSocket);
  this.generator = Utils.getService("@mozilla.org/uuid-generator;1", "nsIUUIDGenerator");
  this.enableNativeEvents = null;

  // Force our cert override service to be loaded - otherwise, it will not be
  // loaded and cause a "too deep recursion" error.
  var overrideService = Components.classes["@mozilla.org/security/certoverride;1"]
      .getService(Components.interfaces.nsICertOverrideService);

  var dispatcher_ = new Dispatcher();

  try {
  this.server_ = Utils.newInstance("@mozilla.org/server/jshttp;1", "nsIHttpServer");
  } catch (e) {
      Logger.dumpn(e);
  }

  this.server_.registerGlobHandler(".*/hub/.*", { handle: function(request, response) {
    response.processAsync();
    dispatcher_.dispatch(new Request(request), new Response(response));
  }});
}


WebDriverServer.prototype.newDriver = function(window) {
  if (null == this.useNativeEvents) {
    var prefs =
        Utils.getService("@mozilla.org/preferences-service;1", "nsIPrefBranch");
    if (!prefs.prefHasUserValue("webdriver_enable_native_events")) {
      Logger.dumpn('webdriver_enable_native_events not set; defaulting to false');
    }
    this.enableNativeEvents =
    prefs.prefHasUserValue("webdriver_enable_native_events") ?
      prefs.getBoolPref("webdriver_enable_native_events") : false;
    Logger.dumpn('Using native events: ' + this.enableNativeEvents);
  }
  window.fxdriver = new FirefoxDriver(this, this.enableNativeEvents, window);
  return window.fxdriver;
};


WebDriverServer.prototype.getNextId = function() {
  return this.generator.generateUUID().toString();
};


WebDriverServer.prototype.startListening = function(port) {
  if (!port) {
    var prefs =
        Utils.getService("@mozilla.org/preferences-service;1", "nsIPrefBranch");

    port = prefs.prefHasUserValue("webdriver_firefox_port") ?
           prefs.getIntPref("webdriver_firefox_port") : 7055;
  }

  if (!this.isListening) {
    this.server_.start(port);
    this.isListening = true;
  }
};


WebDriverServer.prototype.QueryInterface = function(aIID) {
  if (!aIID.equals(nsISupports))
    throw Components.results.NS_ERROR_NO_INTERFACE;
  return this;
};


WebDriverServer.prototype.createInstance = function(ignore1, ignore2, ignore3) {
  var port = WebDriverServer.readPort();
  this.startListening(port);
};
