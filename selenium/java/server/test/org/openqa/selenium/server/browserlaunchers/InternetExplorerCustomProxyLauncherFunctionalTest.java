package org.openqa.selenium.server.browserlaunchers;

import junit.framework.TestCase;

import org.openqa.selenium.browserlaunchers.AsyncExecute;
import org.openqa.selenium.server.RemoteControlConfiguration;

/**
 * {@link org.openqa.selenium.server.browserlaunchers.InternetExplorerCustomProxyLauncher} functional test class.
 */
public class InternetExplorerCustomProxyLauncherFunctionalTest extends TestCase {

    public void testCanLaunchASingleBrowser() {
        final InternetExplorerCustomProxyLauncher launcher;

        launcher = new InternetExplorerCustomProxyLauncher(BrowserOptions.newBrowserOptions(), new RemoteControlConfiguration(), "aSessionId", (String) null);
        launcher.launch("http://www.google.com/");
        AsyncExecute.sleepTightInSeconds(5);
        launcher.close();
    }

    public void testCanLaunchTwoBrowsersInSequence() {
        final InternetExplorerCustomProxyLauncher firstLauncher;
        final InternetExplorerCustomProxyLauncher secondLauncher;

        firstLauncher = new InternetExplorerCustomProxyLauncher(BrowserOptions.newBrowserOptions(), new RemoteControlConfiguration(), "firstSessionId", (String) null);
        secondLauncher = new InternetExplorerCustomProxyLauncher(BrowserOptions.newBrowserOptions(), new RemoteControlConfiguration(), "secondSessionId", (String) null);
        firstLauncher.launch("http://www.google.com/");
        AsyncExecute.sleepTightInSeconds(5);
        firstLauncher.close();
        secondLauncher.launch("http://www.google.com/");
        AsyncExecute.sleepTightInSeconds(5);
        secondLauncher.close();
    }

    public void testCanLaunchTwoBrowsersInterleaved() {
        final InternetExplorerCustomProxyLauncher firstLauncher;
        final InternetExplorerCustomProxyLauncher secondLauncher;

        firstLauncher = new InternetExplorerCustomProxyLauncher(BrowserOptions.newBrowserOptions(), new RemoteControlConfiguration(), "firstSessionId", (String) null);
        secondLauncher = new InternetExplorerCustomProxyLauncher(BrowserOptions.newBrowserOptions(), new RemoteControlConfiguration(), "secondSessionId", (String) null);
        firstLauncher.launch("http://www.google.com/");
        secondLauncher.launch("http://www.google.com/");
        AsyncExecute.sleepTightInSeconds(5);
        firstLauncher.close();
        secondLauncher.close();
    }

}
