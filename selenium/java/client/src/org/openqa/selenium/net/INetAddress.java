/*
Copyright 2007-2010 WebDriver committers
Copyright 2007-2010 Google Inc.

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
package org.openqa.selenium.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class INetAddress {
  private final String hostName;
  private final String hostAddress;
  private final boolean loopbackAddress;
  private final InetAddress inetAddress;

  public INetAddress(InetAddress inetAddress) {
    this.inetAddress = inetAddress;
    this.hostName = inetAddress.getHostName();
    hostAddress = inetAddress.getHostAddress();
    this.loopbackAddress = inetAddress.isLoopbackAddress();
  }

  @SuppressWarnings({"AssignmentToNull"})
  INetAddress(String hostName, String hostAddress, boolean loopbackAddress) {
    this.inetAddress = null;
    this.hostName = hostName;
    this.hostAddress = hostAddress;
    this.loopbackAddress = loopbackAddress;
  }

  public boolean isLoopbackAddress() {
    return loopbackAddress;
  }

  public boolean isIPv6Address() {
    return hostAddress.indexOf(":") != -1;
  }

  public boolean isIPv4Address() {
    return !isIPv6Address();
  }

  public String getHostAddress() {
    return hostAddress;
  }

  public String getHostName() {
    return hostName;
  }

  public InetSocketAddress createInetSocketAddress(int port) {
    if (inetAddress != null) {
      return new InetSocketAddress(inetAddress, port);
    } else {
      return new InetSocketAddress(hostName, port);
    }
  }
}
