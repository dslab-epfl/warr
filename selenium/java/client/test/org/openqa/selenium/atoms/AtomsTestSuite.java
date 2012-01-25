package org.openqa.selenium.atoms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ContextAction;
import net.sourceforge.htmlunit.corejs.javascript.ContextFactory;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.IOException;
import java.net.URL;

@RunWith(BlockJUnit4ClassRunner.class)
public class AtomsTestSuite {

  private static final String FRAGMENT_PATH = "/scripts/executeScript.js";
  private static String fragment;

  private ScriptableObject global;

  @BeforeClass
  public static void loadFragment() {
    URL atomUrl = AtomsTestSuite.class.getResource(FRAGMENT_PATH);
    assertNotNull("Fragment not found", atomUrl);
    try {
      fragment = Resources.toString(atomUrl, Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  public void prepareGlobalObject() {
    ContextFactory.getGlobal().call(new ContextAction() {
      public Object run(Context context) {
        global = context.initStandardObjects();
        global.defineProperty("_", 1234, ScriptableObject.EMPTY);
        assertEquals(1234, context.evaluateString(global, "_", "", 1, null));
        return null;
      }
    });
  }

  /** http://code.google.com/p/selenium/issues/detail?id=1333 */
  @Test
  public void fragmentWillNotLeakVariablesToEnclosingScopes() {
    ContextFactory.getGlobal().call(new ContextAction() {
      public Object run(Context context) {
        context.evaluateString(global, "(" + fragment + ")()", FRAGMENT_PATH, 1, null);
        assertEquals(1234, context.evaluateString(global, "_", "", 1, null));

        context.evaluateString(global, "(" + fragment + ").call(this)", FRAGMENT_PATH, 1, null);
        assertEquals(1234, context.evaluateString(global, "_", "", 1, null));

        context.evaluateString(global, "(" + fragment + ").apply(this,[])", FRAGMENT_PATH, 1, null);
        assertEquals(1234, context.evaluateString(global, "_", "", 1, null));

        context.evaluateString(global, "(" + fragment + ").call(null)", FRAGMENT_PATH, 1, null);
        assertEquals(1234, context.evaluateString(global, "_", "", 1, null));

        context.evaluateString(global, "(" + fragment + ").apply(null,[])", FRAGMENT_PATH, 1, null);
        assertEquals(1234, context.evaluateString(global, "_", "", 1, null));

        context.evaluateString(global, "(" + fragment + ").call({})", FRAGMENT_PATH, 1, null);
        assertEquals(1234, context.evaluateString(global, "_", "", 1, null));
        return null;
      }
    });
  }

  @Test
  public void nestedFragmentsShouldNotLeakVariables() {
    ContextFactory.getGlobal().call(new ContextAction() {
      public Object run(Context context) {
        // executeScript atom recursing on itself to execute "return 1+2".
        // Should result in {status:0,value:{status:0,value:3}}
        // Global scope should not be modified.
        String nestedScript = String.format("(%s).call(this, %s, ['return 1+2;'], false)",
            fragment, fragment);

        NativeObject result = (NativeObject) context.evaluateString(global, nestedScript,
            FRAGMENT_PATH, 1, null);

        assertThat(result.get("status"), instanceOf(Number.class));
        assertEquals(0, ((Number) result.get("status")).intValue());
        assertThat(result.get("value"), instanceOf(NativeObject.class));

        result = (NativeObject) result.get("value");
        assertThat(result.get("status"), instanceOf(Number.class));
        assertEquals(0, ((Number) result.get("status")).intValue());
        assertThat(result.get("value"), instanceOf(Number.class));
        assertEquals(3, ((Number) result.get("value")).intValue());

        assertEquals(1234, context.evaluateString(global, "_", "", 1, null));
        return null;
      }
    });
  }
}
