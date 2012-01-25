package com.thoughtworks.selenium.thirdparty;

import com.thoughtworks.selenium.InternalSelenseTestBase;
import org.testng.annotations.Test;

public class SSLWellsFargoTest extends InternalSelenseTestBase {
    
    @Test(dataProvider = "system-properties")
    public void testWellsFargo() {
        selenium.open("https://www.wellsfargo.com");
        assertEquals("Wells Fargo Home Page", selenium.getTitle());
    }
}
