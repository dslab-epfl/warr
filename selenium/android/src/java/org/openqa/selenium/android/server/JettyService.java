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

package org.openqa.selenium.android.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import com.google.common.io.ByteStreams;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openqa.selenium.android.AndroidDriver;
import org.openqa.selenium.android.Logger;
import org.openqa.selenium.android.Platform;
import org.openqa.selenium.android.app.R;

public class JettyService extends Service {
  private static final String LOG_TAG = JettyService.class.getName();
  private NotificationManager notificationManager;
  private Server server;
  private int port = 8080;

  private PowerManager.WakeLock wakeLock;

  private IBinder binder;
  
  @Override
  public void onCreate() {
    binder = new WebDriverBinder(this);
    startServer();
    super.onCreate();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }
  
  /**
   * Android Service Start
   * 
   * @see android.app.Service#onStart(android.content.Intent, int)
   */
  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
  }
  
  /**
   * Android Service destroy
   * 
   * @see android.app.Service#onDestroy()
   */
  @Override
  public void onDestroy() {
    try {
      if (wakeLock != null) {
        wakeLock.release();
        wakeLock = null;
      }

      if (server != null) {
        stopJetty();
        // Cancel the persistent notification.
        notificationManager.cancel(R.string.jetty_started);
        // Tell the user we stopped.
        Toast.makeText(this, getText(R.string.jetty_stopped), Toast.LENGTH_SHORT).show();
        Logger.log(Log.INFO, LOG_TAG, "Jetty stopped");
      } else {
        Toast.makeText(JettyService.this, R.string.jetty_not_running, Toast.LENGTH_SHORT).show();
      }
    } catch (Exception e) {
      Logger.log(Log.INFO, LOG_TAG, "Error stopping jetty" + e.getMessage());
      Toast.makeText(this, getText(R.string.jetty_not_stopped), Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onLowMemory() {
    Logger.log(Log.INFO, LOG_TAG, "Low on memory");
    super.onLowMemory();
  }

  /**
   * Get a reference to the Jetty Server instance
   */
  public Server getServer() {
    return server;
  }

  protected void configureConnectors() {
    if (server != null) {
      // Workaround a Froyo bug
      // http://code.google.com/p/android/issues/detail?id=9431
      if (Platform.FROYO == Platform.sdk()) {
        System.setProperty("java.net.preferIPv6Addresses", "false");
      }
      SelectChannelConnector nioConnector = new SelectChannelConnector();
      nioConnector.setUseDirectBuffers(false);
      nioConnector.setPort(port);
      nioConnector.setAcceptors(1);
      server.addConnector(nioConnector);
    }
  }

  protected void configureHandlers() {
    if (server != null) {
      org.eclipse.jetty.servlet.ServletContextHandler root =
          new org.eclipse.jetty.servlet.ServletContextHandler(server, "/hub",
              org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS);
      root.addServlet(new ServletHolder(new AndroidDriverServlet()), "/*");

      org.eclipse.jetty.servlet.ServletContextHandler resources =
        new org.eclipse.jetty.servlet.ServletContextHandler(server, "/resources",
            org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS);
      resources.addServlet(new ServletHolder(new HttpServlet() {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
          long a=System.currentTimeMillis();
          response.setContentType("text/plain");
          InputStream in = null;
          try {
            in = JettyService.this.getResources().openRawResource(R.raw.javascript_xpath);
            ByteStreams.copy(in, response.getOutputStream());
          } catch (Exception e) {
            Logger.log(Log.ERROR, LOG_TAG, "Could not open resources" + e.getMessage());
          } finally {
            if (in != null) {
              in.close();
            }
          }
          Logger.log(Log.DEBUG, LOG_TAG,
              "Loading resource took " + (System.currentTimeMillis()-a) + " ms");
        }
      }), "/*");
      
      HandlerList handlers = new HandlerList();
      handlers.setHandlers(
          new org.eclipse.jetty.server.Handler[] {resources,root, new DefaultHandler()});
      server.setHandler(handlers);

    }
  }

  public void startServer() {
    if (server!= null && server.isRunning()) {
      Toast.makeText(JettyService.this, R.string.jetty_already_started, Toast.LENGTH_SHORT).show();
      return;
    }

    try {
      // Get a wake lock to stop the cpu going to sleep
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "IJetty");
      wakeLock.acquire();

      AndroidDriver.setContext(this);

      System.setProperty("org.mortbay.log.class", "org.mortbay.log.AndroidLog");
      server = new Server();

      configureConnectors();
      configureHandlers();

      server.start();

      HttpGenerator.setServerVersion("WebDriver jetty");

      notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      Toast.makeText(JettyService.this, R.string.jetty_started, Toast.LENGTH_SHORT).show();

      Logger.log(Log.INFO, LOG_TAG, "Jetty started");
    } catch (Exception e) {
      Logger.log(Log.ERROR, LOG_TAG, "Error starting jetty" + e);
      Toast.makeText(this, getText(R.string.jetty_not_started), Toast.LENGTH_SHORT).show();
      throw new RuntimeException("Jetty failed to start!");
    }
  }

  protected void stopJetty() throws Exception {
    Logger.log(Log.DEBUG, LOG_TAG, "Jetty stopping");
    server.stop();
    server.join();
    server = null;
  }
}
