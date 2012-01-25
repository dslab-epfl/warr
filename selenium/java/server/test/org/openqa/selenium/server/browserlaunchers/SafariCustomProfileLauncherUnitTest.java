package org.openqa.selenium.server.browserlaunchers;

import static org.easymock.classextension.EasyMock.createStrictMock;
import static org.easymock.classextension.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import org.junit.Test;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.browserlaunchers.locators.BrowserInstallation;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SslCertificateGenerator;


public class SafariCustomProfileLauncherUnitTest {
	
	private AbstractBrowserLauncher launcher;
	private Capabilities browserOptions = BrowserOptions.newBrowserOptions();
	private RemoteControlConfiguration remoteConfiguration = new RemoteControlConfiguration();
	private SslCertificateGenerator generator;
	
	@Test(expected=InvalidBrowserExecutableException.class)
	public void constructor_invalidBrowserInstallationCausesException() throws Exception {
		launcher = new SafariCustomProfileLauncher(browserOptions, remoteConfiguration, "session", "invalid");
	}
	
	@Test
	public void launchRemoteSession_generatesSslCertsIfBrowserSideLogEnabled() throws Exception {
		String location = null;
		
		generator = createStrictMock(SslCertificateGenerator.class);
		generator.generateSSLCertsForLoggingHosts();
		expectLastCall().once();
		
		remoteConfiguration.setSeleniumServer(generator);
		((DesiredCapabilities) browserOptions).setCapability("browserSideLog", true);
		
		launcher = new SafariCustomProfileLauncher(browserOptions, remoteConfiguration, "session", location) {
			@Override
			protected void launch(String url) {
			}
			
			@Override
			protected BrowserInstallation locateSafari(String location) {
					return new BrowserInstallation("", "");
			}
		};
		
		replay(generator);
		launcher.launchRemoteSession("http://url");
		verify(generator);
	}
	
}
