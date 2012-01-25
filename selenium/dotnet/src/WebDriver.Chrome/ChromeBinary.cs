using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using Microsoft.Win32;

namespace OpenQA.Selenium.Chrome
{
    /// <summary>
    /// Provides a mechanism to find the Chrome Binary
    /// </summary>
    public class ChromeBinary
    {
        private static readonly string[] chromePaths = new string[]
        {
            "/usr/bin/google-chrome",
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            string.Concat("/Users/", Environment.UserName, "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")
        };

        private const string LocalCommandExecutorServerUrl = "http://localhost:{0}/chromeCommandExecutor";
        private const int ShutdownWaitInterval = 2000;
        private const int StartWaitInterval = 2500;
        private static int linearStartWaitCoefficient = 1;
        private static string chromeFile = string.Empty;
        private ChromeProfile profile;
        private ChromeExtension extension;
        private Process chromeProcess;
        private int listeningPort;
        private StringBuilder customArgs = new StringBuilder();

        /// <summary>
        /// Initializes a new instance of the ChromeBinary class using the given <see cref="ChromeProfile"/> and <see cref="ChromeExtension"/>.
        /// </summary>
        /// <param name="profile">The Chrome profile to use.</param>
        /// <param name="extension">The extension to launch Chrome with.</param>
        internal ChromeBinary(ChromeProfile profile, ChromeExtension extension)
            : this(profile, extension, 0)
        {
        }

        /// <summary>
        /// Initializes a new instance of the ChromeBinary class using the given <see cref="ChromeProfile"/>, <see cref="ChromeExtension"/> and port value.
        /// </summary>
        /// <param name="profile">The Chrome profile to use.</param>
        /// <param name="extension">The extension to launch Chrome with.</param>
        /// <param name="port">The port on which to listen for commands.</param>
        internal ChromeBinary(ChromeProfile profile, ChromeExtension extension, int port)
        {
            this.profile = profile;
            this.extension = extension;
            if (port == 0)
            {
                this.FindFreePort();
            }
            else
            {
                this.listeningPort = port;
            }
        }

        /// <summary>
        /// Gets the port number on which the <see cref="ChromeExtension"/> should listen for commands.
        /// </summary>
        public int Port
        {
            get { return this.listeningPort; }
        }

        private static string ChromePathFromRegistry
        {
            get
            {
                return Registry.LocalMachine.OpenSubKey(
                                    "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe").GetValue(string.Empty).
                                    ToString();
            }
        }

        private string Arguments
        {
            get
            {
                StringBuilder argumentString = new StringBuilder();
                argumentString.Append(" --load-extension=\"").Append(this.extension.ExtensionDirectory).Append("\"");
                argumentString.Append(" --activate-on-launch");
                argumentString.Append(" --homepage=about:blank");
                argumentString.Append(" --no-first-run");
                argumentString.Append(" --disable-hang-monitor");
                argumentString.Append(" --disable-popup-blocking");
                argumentString.Append(" --disable-prompt-on-repost");
                argumentString.Append(" --no-default-browser-check ");
                argumentString.Append(this.profile.UntrustedCertificatesCommandLineArgument);
                argumentString.Append(this.customArgs);

                if (this.profile != ChromeProfile.DefaultProfile)
                {
                    argumentString.Append(" --user-data-dir=\"").Append(this.profile.ProfileDirectory).Append("\"");
                }

                argumentString.Append(" ").Append(string.Format(CultureInfo.InvariantCulture, LocalCommandExecutorServerUrl, this.listeningPort));

                return argumentString.ToString();
            }
        }

        /// <summary>
        /// Increases the wait time used for starting the Chrome process.
        /// </summary>
        /// <param name="diff">Interval by which to increase the wait time.</param>
        public static void IncrementStartWaitInterval(int diff)
        {
            linearStartWaitCoefficient += diff;
        }

        /// <summary>
        /// Adds custom arguments to the command line.
        /// </summary>
        /// <param name="customArguments">The arguments to add.</param>
        public void AddCustomArguments(string customArguments)
        {
            this.customArgs.Append(" ").Append(customArguments);
        }

        /// <summary>
        ///  Starts the Chrome process for WebDriver. Assumes the passed directories exist.
        /// </summary>
        /// <exception cref="WebDriverException">When it can't launch will throw an error</exception>
        public void Start()
        {
            try
            {
                string chromeFileLocation = GetChromeFile();
                string commandLineArguments = this.Arguments;
                ProcessStartInfo startInfo = new ProcessStartInfo(chromeFileLocation, commandLineArguments);
                startInfo.UseShellExecute = false;
                this.chromeProcess = Process.Start(startInfo);
            }
            catch (IOException e)
            { // TODO(AndreNogueira): Check exception type thrown when process.start fails
                throw new WebDriverException("Could not start Chrome process", e);
            }

            Thread.Sleep(StartWaitInterval * linearStartWaitCoefficient);
        }

        /// <summary>
        /// Kills off the Browser Instance
        /// </summary>
        public void Kill()
        {
            if ((this.chromeProcess != null) && !this.chromeProcess.HasExited)
            {
                // Ask nicely to close.
                this.chromeProcess.CloseMainWindow();
                this.chromeProcess.WaitForExit(ShutdownWaitInterval);

                // If it still hasn't closed, be rude.
                if (!this.chromeProcess.HasExited)
                {
                    this.chromeProcess.Kill();
                }

                this.chromeProcess = null;
                if (this.profile.DeleteProfileOnExit)
                {
                    DeleteProfileDirectory(this.profile.ProfileDirectory);
                }
            }
        }

        /// <summary>
        /// Locates the Chrome executable on the current platform. First looks in the webdriver.chrome.bin property, then searches
        /// through the default expected locations.
        /// </summary>
        /// <returns>chrome.exe path</returns>
        private static string GetChromeFile()
        {
            if (!IsChromeBinaryLocationKnown())
            {
                string chromeFileSystemProperty = Environment.GetEnvironmentVariable("webdriver.chrome.bin");
                if (chromeFileSystemProperty != null)
                {
                    chromeFile = chromeFileSystemProperty;
                }
                else
                {
                    if (Platform.CurrentPlatform.IsPlatformType(PlatformType.XP) || Platform.CurrentPlatform.IsPlatformType(PlatformType.Vista))
                    {
                        try
                        {
                            chromeFile = ChromePathFromRegistry;
                        }
                        catch (NullReferenceException)
                        {
                            if (Platform.CurrentPlatform.IsPlatformType(PlatformType.XP))
                            {
                                chromeFile =
                                    Path.Combine(
                                        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                                        "Google\\Chrome\\Application\\chrome.exe");
                            }
                            else
                            {
                                chromeFile = Path.Combine(Path.GetTempPath(), "..\\Google\\Chrome\\Application\\chrome.exe");
                            }
                        }
                    }
                    else if (Platform.CurrentPlatform.IsPlatformType(PlatformType.Unix))
                    {
                        // Thanks to a bug in Mono Mac and Linux will be treated the same  https://bugzilla.novell.com/show_bug.cgi?id=515570 but adding this in case
                        string chromeFileString = string.Empty;
                        bool foundPath = false;
                        foreach (string path in chromePaths)
                        {
                            FileInfo binary = new FileInfo(path);
                            if (binary.Exists)
                            {
                                chromeFileString = binary.FullName;
                                foundPath = true;
                                break;
                            }
                        }

                        if (!foundPath)
                        {
                            throw new WebDriverException("Couldn't locate Chrome. Set webdriver.chrome.bin");
                        }

                        chromeFile = chromeFileString;
                    }
                    else
                    {
                        throw new WebDriverException(
                            "Unsupported operating system. Could not locate Chrome.  Set webdriver.chrome.bin");
                    }
                }
            }

            return chromeFile;
        }

        private static bool IsChromeBinaryLocationKnown()
        {
            return !string.IsNullOrEmpty(chromeFile) && File.Exists(chromeFile);
        }

        private static void DeleteProfileDirectory(string directoryToDelete)
        {
            int numberOfRetries = 0;
            while (Directory.Exists(directoryToDelete) && numberOfRetries < 10)
            {
                try
                {
                    Directory.Delete(directoryToDelete, true);
                }
                catch (IOException)
                {
                    // If we hit an exception (like file still in use), wait a half second
                    // and try again. If we still hit an exception, go ahead and let it through.
                    System.Threading.Thread.Sleep(500);
                }
                catch (UnauthorizedAccessException)
                {
                    // If we hit an exception (like file still in use), wait a half second
                    // and try again. If we still hit an exception, go ahead and let it through.
                    System.Threading.Thread.Sleep(500);
                }
                finally
                {
                    numberOfRetries++;
                }

                if (Directory.Exists(directoryToDelete))
                {
                    Console.WriteLine("Unable to delete profile directory '{0}'", directoryToDelete);
                }
            }
        } 

        private void FindFreePort()
        {
            // Locate a free port on the local machine by binding a socket to
            // an IPEndPoint using IPAddress.Any and port 0. The socket will
            // select a free port.
            Socket portSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            IPEndPoint socketEndPoint = new IPEndPoint(IPAddress.Any, 0);
            portSocket.Bind(socketEndPoint);
            socketEndPoint = (IPEndPoint)portSocket.LocalEndPoint;
            this.listeningPort = socketEndPoint.Port;
            portSocket.Close();
        }
    }
}
