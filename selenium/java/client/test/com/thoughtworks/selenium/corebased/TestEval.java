package com.thoughtworks.selenium.corebased;

import com.thoughtworks.selenium.InternalSelenseTestBase;
import org.junit.Test;

public class TestEval extends InternalSelenseTestBase {
	@Test
  public void testEval() throws Exception {
		selenium.open("../tests/html/test_open.html");
		assertEquals(selenium.getEval("window.document.title"), "Open Test");
	}
}
