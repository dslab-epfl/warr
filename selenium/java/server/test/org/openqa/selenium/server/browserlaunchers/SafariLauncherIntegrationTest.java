package org.openqa.selenium.server.browserlaunchers;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.openqa.jetty.log.LogFactory;
import org.openqa.selenium.browserlaunchers.AsyncExecute;
import org.openqa.selenium.server.RemoteControlConfiguration;

/**
 * {@link org.openqa.selenium.server.browserlaunchers.SafariCustomProfileLauncher} integration test class.
 */
public class SafariLauncherIntegrationTest extends TestCase {

    private static final Log LOGGER = LogFactory.getLog(SafariLauncherIntegrationTest.class);
    private static final int SECONDS = 1000;
    private static final int WAIT_TIME = 15 * SECONDS;

    public void testLauncherWithDefaultConfiguration() throws Exception {
        final SafariCustomProfileLauncher launcher;

        launcher = new SafariCustomProfileLauncher(BrowserOptions.newBrowserOptions(), new RemoteControlConfiguration(), "CUST", null);
        launcher.launch("http://www.google.com");
        int seconds = 15;
        LOGGER.info("Killing browser in " + Integer.toString(seconds) + " seconds");
        AsyncExecute.sleepTight(WAIT_TIME);
        launcher.close();
        LOGGER.info("He's dead now, right?");
    }

    public void testLauncherWithHonorSystemProxyEnabled() throws Exception {
        final SafariCustomProfileLauncher launcher;
        final RemoteControlConfiguration configuration;

        configuration = new RemoteControlConfiguration();
        configuration.setHonorSystemProxy(true);
        
        launcher = new SafariCustomProfileLauncher(BrowserOptions.newBrowserOptions(), configuration, "CUST", null);
        launcher.launch("http://www.google.com");
        int seconds = 15;
        LOGGER.info("Killing browser in " + Integer.toString(seconds) + " seconds");
        AsyncExecute.sleepTight(WAIT_TIME);
        launcher.close();
        LOGGER.info("He's dead now, right?");
    }

}
