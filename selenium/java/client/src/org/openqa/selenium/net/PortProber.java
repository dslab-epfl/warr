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

package org.openqa.selenium.net;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

@SuppressWarnings({"UtilityClass"})
public class PortProber {
  private static final Random random = new Random();

  private PortProber() {
  }

  public static int findFreePort() {
    for (int i = 0; i < 5; i++) {
      int seedPort = createAcceptablePort();
      int suggestedPort = checkPortIsFree(seedPort);
      if (suggestedPort != -1) {
        return suggestedPort;
      }
    }
    throw new RuntimeException("Unable to find a free port");
  }

  public static Callable<Integer> freeLocalPort(final int port) {
    return new Callable<Integer>() {

      public Integer call() throws Exception {
        if (checkPortIsFree(port) != -1) {
          return port;
        }
        return null;
      }
    };
  }

  private static int createAcceptablePort() {
    synchronized (random) {
      int seed = random.nextInt();
      // avoid protected ports
      final int FIRST_PORT = 1025;
      final int LAST_PORT = 65534;
      final int randomInt = Math.abs(random.nextInt());
      seed = (randomInt % (LAST_PORT - FIRST_PORT + 1)) + FIRST_PORT;
      return seed;
    }
  }

  private static int checkPortIsFree(int port) {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(port);
      int localPort = socket.getLocalPort();
      socket.close();
      return localPort;
    } catch (IOException e) {
      return -1;
    }
  }

  public static boolean pollPort(int port) {
    return pollPort(port, 15, SECONDS);
  }

  public static boolean pollPort(int port, int timeout, TimeUnit unit) {
    long end = System.currentTimeMillis() + unit.toMillis(timeout);
    while (System.currentTimeMillis() < end) {
      try {
        Socket socket = new Socket("localhost", port);
        socket.close();
        return true;
      } catch (ConnectException e) {
        // Ignore this
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return false;
  }
}
