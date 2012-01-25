package com.thoughtworks.selenium.corebased;

import com.thoughtworks.selenium.InternalSelenseTestBase;
import org.junit.Test;

public class TestFramesClickJavascriptHref extends InternalSelenseTestBase {
	@Test public void testFramesClickJavascriptHref() throws Exception {
		selenium.open("../tests/html/Frames.html");
		selenium.selectFrame("mainFrame");
		selenium.open("../tests/html/test_click_javascript_page.html");
		selenium.selectFrame("relative=top");
		selenium.click("link");
		verifyEquals(selenium.getAlert(), "link clicked: foo");
	}
}
