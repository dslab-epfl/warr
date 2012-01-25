package org.openqa.selenium.chrome;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.internal.CircularOutputStream;
import org.openqa.selenium.remote.internal.SubProcess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.openqa.selenium.Proxy.ProxyType;

public class ChromeBinary {

  private static final String CHROME_LOG_FILE_PROPERTY = "webdriver.chrome.logFile";
  private static final int BACKOFF_INTERVAL = 2500;

  private volatile int linearBackoffCoefficient = 1;

  private final ChromeProfile profile;
  private final ChromeExtension extension;
  private int port;
  private SubProcess chromeProcess;
  
  private List<String> customFlags = new ArrayList<String>();

  protected String chromeBinaryLocation = null;

  /**
   * @param profile The Chrome profile to use.
   * @param extension The extension to launch Chrome with.
   * @throws WebDriverException If an error occurs locating the Chrome executable.
   * @see ChromeBinary(ChromeProfile, ChromeExtension, int)
   */
  public ChromeBinary(ChromeProfile profile, ChromeExtension extension) {
    this(profile, extension, 0);
  }

  /**
   * Creates a new instance for managing an instance of Chrome using the given
   * {@code profile} and {@code extension}.
   *
   * @param profile The Chrome profile to use.
   * @param extension The extension to launch Chrome with.
   * @param port Which port to start Chrome on, or 0 for any free port.
   * @throws WebDriverException If an error occurs locating the Chrome executable.
   */
  public ChromeBinary(ChromeProfile profile, ChromeExtension extension, int port) {
    this.profile = profile;
    this.extension = extension;
    this.port = port;
  }

  private SubProcess prepareProcess() {
    String serverUrl = String.format("http://localhost:%d/chromeCommandExecutor", this.port);

    ProcessBuilder builder;
    try {
      List<String> commandline = getCommandline(serverUrl);
      builder = new ProcessBuilder(commandline);
    } catch (IOException e) {
      throw new WebDriverException(e);
    }

    File logFile = getLogFile();
    return logFile == null
        ? new SubProcess(builder)
        : new SubProcess(builder, new CircularOutputStream(logFile));
  }

  private static File getLogFile() {
    String logFile = System.getProperty(CHROME_LOG_FILE_PROPERTY);
    return logFile == null ? null : new File(logFile);
  }

  public void addCustomBinaryFlag(String flag) {
    this.customFlags.add(flag);
  }
  
  public ChromeProfile getProfile() {
    return profile;
  }

  public ChromeExtension getExtension() {
    return extension;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Starts the Chrome process for WebDriver.
   */
  public void start() {
    if (chromeProcess == null) {
      chromeProcess = prepareProcess();
    }
    chromeProcess.launch();
    try {
      Thread.sleep(BACKOFF_INTERVAL * linearBackoffCoefficient);
    } catch (InterruptedException e) {
      //Nothing sane to do here
    }
  }

  @VisibleForTesting List<String> getCommandline(String serverUrl) throws IOException {
    List<String> commandline = Lists.newArrayList(
        getChromeBinaryLocation(),
        "--load-extension=" + extension.getDirectory().getAbsolutePath(),
        "--activate-on-launch",
        "--homepage=about:blank",
        "--no-first-run",
        "--disable-hang-monitor",
        "--disable-popup-blocking",
        "--disable-prompt-on-repost",
        "--no-default-browser-check",
        "--disable-translate",
        profile.getUntrustedCertificatesFlag()
    );
    commandline.addAll(this.customFlags);
    if (!profile.equals(ChromeProfile.DEFAULT_PROFILE)) {
      commandline.add("--user-data-dir=" + profile.getDirectory().getAbsolutePath());
    }
    appendProxyArguments(commandline)
        .add(serverUrl);
    return commandline;
  }

  private List<String> appendProxyArguments(List<String> commandline) {
    Proxy proxy = profile.getProxy();
    if (proxy == null) {
      return commandline;
    }
    if (proxy.getProxyAutoconfigUrl() != null) {
      commandline.add("--proxy-pac-url=" + proxy.getProxyAutoconfigUrl());
    } else if (proxy.getHttpProxy() != null) {
      commandline.add("--proxy-server=" + proxy.getHttpProxy());
    } else if (proxy.isAutodetect()) {
      commandline.add("--proxy-auto-detect");
    } else if (proxy.getProxyType() == ProxyType.DIRECT) {
      commandline.add("--no-proxy-server");
    } else if (proxy.getProxyType() != ProxyType.SYSTEM) {
      throw new IllegalStateException("Unsupported proxy setting");
    }
    return commandline;
  }

  /**
   * @return Whether the Chrome process managed by this instance is still
   *     running.
   */
  public boolean isRunning() {
    return chromeProcess.isRunning();
  }

  /**
   * Kills the Chrome process managed by this instance.
   */
  public void kill() {
    if (chromeProcess != null) {
      chromeProcess.shutdown();
    }
  }

  public void incrementBackoffBy(int diff) {
    linearBackoffCoefficient += diff;
  }

  /**
   * Locates the Chrome executable on the current platform.
   * First looks in the webdriver.chrome.bin property, then searches
   * through the default expected locations.
   * @return chrome.exe
   * @throws IOException if file could not be found/accessed
   */
  protected String getChromeBinaryLocation() throws IOException {
    if (!isChromeBinaryLocationKnown()) {
      chromeBinaryLocation = System.getProperty("webdriver.chrome.bin");
      if (chromeBinaryLocation == null) {
        List<String> paths = new ArrayList<String>();
        if (Platform.getCurrent().is(Platform.WINDOWS)) {
          paths.add(getWindowsBinaryLocationFromRegistry());
          paths.add(getDefaultWindowsBinaryLocation());
        } else if (Platform.getCurrent().is(Platform.UNIX)) {
          paths.add("/usr/bin/google-chrome");
          paths.add("/usr/bin/chromium");
          //TODO: Add `which google-chrome` and `which chromium`
        } else if (Platform.getCurrent().is(Platform.MAC)) {
          paths.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
          paths.add("/Users/" + System.getProperty("user.name") + "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        } else {
          throw new WebDriverException("Unsupported operating system.  " +
              "Could not locate Chrome.  Set webdriver.chrome.bin");
        }
        for (String path : paths) {
          if (path == null) {
            continue;
          }
          File binary = new File(path);
          if (binary.exists()) {
            chromeBinaryLocation = binary.getCanonicalFile().getAbsoluteFile().toString();
            break;
          }
        }
      }
      if (!isChromeBinaryLocationKnown()) {
        throw new WebDriverException("Couldn't locate Chrome.  " +
            "Set webdriver.chrome.bin");
      }
    }
    return chromeBinaryLocation;
  }

  protected boolean isChromeBinaryLocationKnown() {
    return chromeBinaryLocation != null && new File(chromeBinaryLocation).exists();
  }

  /**
   * Returns null if couldn't read value from registry
   */
  protected static final String getWindowsBinaryLocationFromRegistry() {
    //TODO: Promote org.openqa.selenium.server.browserlaunchers.WindowsUtils
    //to common and reuse that to read the registry
    if (!Platform.WINDOWS.is(Platform.getCurrent())) {
      throw new UnsupportedOperationException("Cannot get registry value on non-Windows systems");
    }
    try {
      Process process = Runtime.getRuntime().exec(
          "reg query \"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe\" /v \"\"");
      BufferedReader reader = new BufferedReader(new InputStreamReader(
          process.getInputStream()));
      process.waitFor();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("    ")) {
          String[] tokens = line.split("REG_SZ");
          return tokens[tokens.length - 1].trim();
        }
      }
    } catch (IOException e) {
      //Drop through to return null
    } catch (InterruptedException e) {
      //Drop through to return null
    }
    return null;
  }

  protected static final String getDefaultWindowsBinaryLocation() {
    StringBuilder path = new StringBuilder();
    path.append(System.getProperty("user.home"));
    //XXX: Not localised for other languages
    if (Platform.VISTA.is(Platform.getCurrent())) {
      path.append("\\AppData\\Local");
    } else if (Platform.XP.is(Platform.getCurrent())) {
      path.append("\\Local Settings\\Application Data");
    }
    path.append("\\Google\\Chrome\\Application\\chrome.exe");
    return path.toString();
  }
}
