/*
Copyright 2007-2010 WebDriver committers
Copyright 2007-2010 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.openqa.selenium.interactions;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

/**
 * Tests the CompositeAction class
 *
 */
public class TestCompositeAction extends MockObjectTestCase {
  public void testAddingActions() {
    CompositeAction sequence = new CompositeAction();
    final Action dummyAction1 = mock(Action.class);
    final Action dummyAction2 = mock(Action.class, "dummy2");
    final Action dummyAction3 = mock(Action.class, "dummy3");

    sequence.addAction(dummyAction1)
        .addAction(dummyAction2)
        .addAction(dummyAction3);
    
    assertEquals(3, sequence.getNumberOfActions());
  }

  public void testInvokingActions() {
    CompositeAction sequence = new CompositeAction();
    final Action dummyAction1 = mock(Action.class);
    final Action dummyAction2 = mock(Action.class, "dummy2");
    final Action dummyAction3 = mock(Action.class, "dummy3");

    sequence.addAction(dummyAction1);
    sequence.addAction(dummyAction2);
    sequence.addAction(dummyAction3);

    checking(new Expectations() {{
      one(dummyAction1).perform();
      one(dummyAction2).perform();
      one(dummyAction3).perform();
    }});
    
    sequence.perform();
  }


}
