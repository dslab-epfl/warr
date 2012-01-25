/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.

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

// Copyright 2008 Google Inc.  All Rights Reserved.

package org.openqa.selenium.remote.server.handler;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.remote.server.DriverSessions;
import org.openqa.selenium.remote.server.JsonParametersAware;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class CookieHandler extends WebDriverHandler implements JsonParametersAware {

  private volatile Map<String, Object> rawCookie;

  public CookieHandler(DriverSessions sessions) {
    super(sessions);
  }

  @SuppressWarnings({"unchecked"})
  public void setJsonParameters(Map<String, Object> allParameters) throws Exception {
    if (allParameters == null) {
      return;
    }
    rawCookie =
        new ConcurrentHashMap<String, Object>((Map<String, Object>) allParameters.get("cookie"));
  }

  protected Cookie createCookie() {
    if (rawCookie == null) {
      return null;
    }

    String name = (String) rawCookie.get("name");
    String value = (String) rawCookie.get("value");
    String path = (String) rawCookie.get("path");
    String domain = (String) rawCookie.get("domain");
    Boolean secure = (Boolean) rawCookie.get("secure");

    Number expiryNum = (Number) rawCookie.get("expiry");
    Date expiry = expiryNum == null ? null : new Date(
        TimeUnit.SECONDS.toMillis(expiryNum.longValue()));

    return new Cookie.Builder(name, value)
        .path(path)
        .domain(domain)
        .isSecure(secure)
        .expiresOn(expiry)
        .build();
  }

}
