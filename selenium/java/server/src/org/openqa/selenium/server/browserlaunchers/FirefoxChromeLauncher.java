/*
 * Copyright 2006 ThoughtWorks, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.openqa.selenium.server.browserlaunchers;


import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.browserlaunchers.AsyncExecute;
import org.openqa.selenium.browserlaunchers.LauncherUtils;
import org.openqa.selenium.browserlaunchers.Proxies;
import org.openqa.selenium.browserlaunchers.locators.BrowserInstallation;
import org.openqa.selenium.browserlaunchers.locators.CombinedFirefoxLocator;
import org.openqa.selenium.os.CommandLine;
import org.openqa.selenium.server.ApplicationRegistry;
import org.openqa.selenium.server.RemoteControlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FirefoxChromeLauncher extends AbstractBrowserLauncher {
  private static final Logger log = Logger.getLogger(FirefoxChromeLauncher.class.getName());

  private File customProfileDir = null;
  private boolean closed = false;
  private BrowserInstallation browserInstallation;
  private Process process = null;

  private boolean changeMaxConnections = false;

  public FirefoxChromeLauncher(Capabilities browserOptions, RemoteControlConfiguration configuration, String sessionId, String browserString)
      throws InvalidBrowserExecutableException {
    this(browserOptions, configuration,
        sessionId, ApplicationRegistry.instance()
            .browserInstallationCache().locateBrowserInstallation(
            "chrome", browserString, new CombinedFirefoxLocator()));
    if (browserInstallation == null) {
      throw new InvalidBrowserExecutableException(
          "The specified path to the browser executable is invalid.");
    }
  }

  public FirefoxChromeLauncher(Capabilities browserOptions, RemoteControlConfiguration configuration, String sessionId, BrowserInstallation browserInstallation) {
    super(sessionId, configuration, browserOptions);

    if (browserInstallation == null) {
      throw new InvalidBrowserExecutableException(
          "The specified path to the browser executable is invalid.");
    }
    this.browserInstallation = browserInstallation;
  }


  /* (non-Javadoc)
  * @see org.openqa.selenium.server.browserlaunchers.AbstractBrowserLauncher#launch(java.lang.String)
  */

  @Override
  protected void launch(String url) {
    final String profilePath;
    final String homePage;

    try {
      homePage = new ChromeUrlConvert().convert(url);
      profilePath = makeCustomProfile(homePage);
      populateCustomProfileDirectory(profilePath);

      log.info("Launching Firefox...");
      CommandLine command = prepareCommand(
          browserInstallation.launcherFilePath(),
          "-profile",
          profilePath
      );
      command.setEnvironmentVariable("NO_EM_RESTART", "1");
      process = command.executeAsync();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void populateCustomProfileDirectory(String profilePath) throws IOException {
    /*
    * The first time we launch Firefox with an empty profile directory,
    * Firefox will launch itself, populate the profile directory, then
    * kill/relaunch itself, so our process handle goes out of date.
    * So, the first time we launch Firefox, we'll start it up at an URL
    * that will immediately shut itself down.
    */
    CommandLine command = prepareCommand(browserInstallation.launcherFilePath(),
        "-profile", profilePath,
        "-silent"
    );
    command.setDynamicLibraryPath(browserInstallation.libraryPath());
    log.info("Preparing Firefox profile...");
    command.execute();
    try {
      waitForFullProfileToBeCreated(20 * 1000);
    } catch (RuntimeException e) {
      command.destroy();
      throw e;
    }
  }

  protected CommandLine prepareCommand(String... commands) {
    CommandLine command = new CommandLine(commands);
    command.setEnvironmentVariable("MOZ_NO_REMOTE", "1");

    // don't set the library path on Snow Leopard
    Platform platform = Platform.getCurrent();
    if (!platform.is(Platform.MAC) || ((platform.is(Platform.MAC))
                                       && platform.getMajorVersion() <= 10
                                       && platform.getMinorVersion() <= 5)) {
        command.setDynamicLibraryPath(browserInstallation.libraryPath());
    }

    return command;
  }

  protected void createCustomProfileDir() {
    customProfileDir = LauncherUtils.createCustomProfileDir(sessionId);
  }

  protected void copyDirectory(File sourceDir, File destDir) {
    LauncherUtils.copyDirectory(sourceDir, destDir);
  }

  protected File initProfileTemplate() {
    File firefoxProfileTemplate;

    String relativeProfile = BrowserOptions
        .getProfile(browserConfigurationOptions);
    if (relativeProfile == null) {
      relativeProfile = "";
    }

    File profilesLocation = getConfiguration().getProfilesLocation();
    if (profilesLocation != null && !"".equals(relativeProfile)) {

      firefoxProfileTemplate = getFileFromParent(profilesLocation, relativeProfile);
      if (!firefoxProfileTemplate.exists()) {
        throw new RuntimeException(
            "The profile specified '" + firefoxProfileTemplate.getAbsolutePath()
            + "' does not exist");
      }
    } else {
      firefoxProfileTemplate = BrowserOptions.getFile(browserConfigurationOptions, "firefoxProfileTemplate");
    }

    if (firefoxProfileTemplate != null) {
      copyDirectory(firefoxProfileTemplate, customProfileDir);
    }

    return firefoxProfileTemplate;
  }

  protected void extractProfileFromJar() throws IOException {
    ResourceExtractor.extractResourcePath(getClass(), "/customProfileDirCUSTFFCHROME",
        customProfileDir);
  }

  protected void copySingleFileWithOverwrite(File sourceFile, File destFile) {
    LauncherUtils.copySingleFileWithOverwrite(sourceFile, destFile, true);
  }

  protected File getFileFromParent(final File parent, String child) {
    return new File(parent, child);
  }

  protected void copyCert8db(final File firefoxProfileTemplate) {
    // Make sure that cert8.db of firefoxProfileTemplate is stored into customProfileDir
    if (firefoxProfileTemplate != null) {
      File sourceCertFile = getFileFromParent(firefoxProfileTemplate, "cert8.db");
      if (sourceCertFile.exists()) {
        File destCertFile = new File(customProfileDir, "cert8.db");
        copySingleFileWithOverwrite(sourceCertFile, destCertFile);
      }
    }
  }

  protected void generatePacAndPrefJs(String homePage) throws IOException {
    browserConfigurationOptions = Proxies.setProxyRequired(browserConfigurationOptions, false);
    if (browserConfigurationOptions.is("captureNetworkTraffic") ||
        browserConfigurationOptions.is("addCustomRequestHeaders") ||
        browserConfigurationOptions.is("trustAllSSLCertificates")) {
      browserConfigurationOptions = Proxies.setProxyEverything(browserConfigurationOptions, true);
      browserConfigurationOptions = Proxies.setProxyRequired(browserConfigurationOptions, true);
    }

    LauncherUtils.generatePacAndPrefJs(customProfileDir, getPort(), homePage,
        changeMaxConnections, getTimeout(), browserConfigurationOptions);
  }

  private String makeCustomProfile(String homePage) throws IOException {

    createCustomProfileDir();

    File firefoxProfileTemplate = initProfileTemplate();

    extractProfileFromJar();

    copyCert8db(firefoxProfileTemplate);

    copyRunnerHtmlFiles();

    changeMaxConnections = browserConfigurationOptions.is("changeMaxConnections");

    generatePacAndPrefJs(homePage);

    return customProfileDir.getAbsolutePath();
  }


  private void copyRunnerHtmlFiles() {
    String guid = "{503A0CD4-EDC8-489b-853B-19E0BAA8F0A4}";
    File extensionDir = new File(customProfileDir, "extensions/" + guid);
    File htmlDir = new File(extensionDir, "chrome");
    htmlDir.mkdirs();

    LauncherUtils.extractHTAFile(htmlDir, getPort(), "/core/TestRunner.html", "TestRunner.html");
    LauncherUtils.extractHTAFile(htmlDir, getPort(), "/core/TestPrompt.html", "TestPrompt.html");
    LauncherUtils.extractHTAFile(htmlDir, getPort(), "/core/RemoteRunner.html",
        "RemoteRunner.html");

  }


  public void close() {
    if (closed) {
      return;
    }
    FileLockRemainedException fileLockException = null;
    if (process != null) {
      try {
        killFirefoxProcess();
      } catch (FileLockRemainedException flre) {
        fileLockException = flre;
      }
    }
    if (customProfileDir != null) {
      try {
        removeCustomProfileDir();
      } catch (RuntimeException e) {
        if (fileLockException != null) {
          log.log(Level.SEVERE, "Couldn't delete custom Firefox profile directory", e);
          log.severe("Perhaps caused by this exception:");
          if (fileLockException != null) {
            log.log(Level.SEVERE, "Perhaps caused by this exception:", fileLockException);
          }
          throw new RuntimeException("Couldn't delete custom Firefox " +
                                     "profile directory, presumably because task kill failed; " +
                                     "see error LOGGER!", e);
        }
        throw e;
      }
    }
    closed = true;
  }

  /**
   * Wrapper to allow for stubbed-out testing *
   */
  protected void removeCustomProfileDir() throws RuntimeException {
    LauncherUtils.deleteTryTryAgain(customProfileDir, 6);
  }

  /**
   * Wrapper to allow for stubbed-out testing *
   */
  protected void killFirefoxProcess() throws FileLockRemainedException {
    log.info("Killing Firefox...");
    int exitValue = AsyncExecute.killProcess(process);
    if (exitValue == 0) {
      log.warning("Firefox seems to have ended on its own (did we kill the real browser???)");
    }
    waitForFileLockToGoAway(0, 500);
  }

  public Process getProcess() {
    return process;
  }

  /**
   * Firefox knows it's running by using a "parent.lock" file in
   * the profile directory.  Wait for this file to go away (and stay gone)
   *
   * @param timeout    max time to wait for the file to go away
   * @param timeToWait minimum time to wait to make sure the file is gone
   * @throws FileLockRemainedException
   */
  private void waitForFileLockToGoAway(long timeout, long timeToWait)
      throws FileLockRemainedException {
    File lock = new File(customProfileDir, "parent.lock");
    for (long start = System.currentTimeMillis(); System.currentTimeMillis() < start + timeout;) {
      AsyncExecute.sleepTight(500);
      if (!lock.exists() && makeSureFileLockRemainsGone(lock, timeToWait)) {
        return;
      }
    }
    if (lock.exists()) {
      throw new FileLockRemainedException("Lock file still present! " + lock.getAbsolutePath());
    }
  }

  /**
   * When initializing the profile, Firefox rapidly starts, stops, restarts and
   * stops again; we need to wait a bit to make sure the file lock is really gone.
   *
   * @param lock       the parent.lock file in the profile directory
   * @param timeToWait minimum time to wait to see if the file shows back
   *                   up again. This is not a timeout; we will always wait this amount of time or more.
   * @return true if the file stayed gone for the entire timeToWait; false if the
   *         file exists (or came back)
   */
  private boolean makeSureFileLockRemainsGone(File lock, long timeToWait) {
    for (long start = System.currentTimeMillis();
         System.currentTimeMillis() < start + timeToWait;) {
      AsyncExecute.sleepTight(500);
      if (lock.exists()) {
        return false;
      }
    }
    return !lock.exists();
  }

  /**
   * Wait for one of the Firefox-generated files to come into existence, then wait
   * for Firefox to exit
   *
   * @param timeout the maximum amount of time to wait for the profile to be created
   */
  private void waitForFullProfileToBeCreated(long timeout) {
    // This will be a characteristic file in the profile
    File testFile = new File(customProfileDir, "extensions.ini");
    long start = System.currentTimeMillis();
    for (; System.currentTimeMillis() < start + timeout;) {

      AsyncExecute.sleepTight(500);
      if (testFile.exists()) {
        break;
      }
    }
    if (!testFile.exists()) {
      throw new RuntimeException("Timed out waiting for profile to be created!");
    }
    // wait the rest of the timeout for the file lock to go away
    long subTimeout = timeout - (System.currentTimeMillis() - start);
    try {
      waitForFileLockToGoAway(subTimeout, 500);
    } catch (FileLockRemainedException e) {
      throw new RuntimeException("Firefox refused shutdown while preparing a profile", e);
    }
  }

  // visible for testing

  protected void setCustomProfileDir(File value) {
    customProfileDir = value;
  }

  // visible for testing

  protected void setProcess(Process p) {
    process = p;
  }

  protected class FileLockRemainedException extends Exception {
    FileLockRemainedException(String message) {
      super(message);
    }
  }

  public static class ChromeUrlConvert {
    public String convert(String httpUrl) throws MalformedURLException {
      String query = LauncherUtils.getQueryString(httpUrl);
      String file = new File(new URL(httpUrl).getPath()).getName();
      return "chrome://src/content/" + file + "?" + query;
    }
  }

  @Override
  // need to specify an absolute resultsUrl
  public void launchHTMLSuite(String suiteUrl, String browserURL) {
    // If navigating to TestPrompt, use the baked-in version instead.
    if (suiteUrl != null && suiteUrl.startsWith("TestPrompt.html?")) {
      suiteUrl =
          suiteUrl.replaceFirst("^TestPrompt\\.html\\?", "chrome://src/content/TestPrompt.html?");
    }
    launch(LauncherUtils.getDefaultHTMLSuiteUrl(browserURL, suiteUrl,
        (!BrowserOptions.isSingleWindow(browserConfigurationOptions)), getPort()));
  }

  @Override
  // need to specify an absolute driverUrl
  public void launchRemoteSession(String browserURL) {
    launch(LauncherUtils.getDefaultRemoteSessionUrl(browserURL, sessionId,
        (!BrowserOptions.isSingleWindow(browserConfigurationOptions)), getPort(),
        browserConfigurationOptions.is("browserSideLog")));
  }

}

