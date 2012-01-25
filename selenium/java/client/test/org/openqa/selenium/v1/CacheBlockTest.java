package org.openqa.selenium.v1;

import com.thoughtworks.selenium.InternalSelenseTestBase;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class CacheBlockTest extends InternalSelenseTestBase {

    @Test(dataProvider = "system-properties")
    public void testCacheBlock() throws Exception {
        selenium.open("/selenium-server/cachedContentTest");
        String text = selenium.getBodyText();
        assertNotNull("body text should not be null", text);
        selenium.stop();
        
        selenium.start();
        selenium.open("/selenium-server/cachedContentTest");
        String text2 = selenium.getBodyText();
        assertFalse("content was cached: " + text, text.equals(text2));
    }
}
