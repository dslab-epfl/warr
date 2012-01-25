/*
Copyright 2010 WebDriver committers
Copyright 2010 Google Inc.

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


package org.openqa.selenium.remote;

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.internal.FindsByCssSelector;

import java.lang.reflect.Method;
import java.util.Map;

public class AddFindsByCss implements AugmenterProvider {
  public Class<?> getDescribedInterface() {
    return FindsByCssSelector.class;
  }

  public InterfaceImplementation getImplementation(Object value) {
    return new InterfaceImplementation() {

      public Object invoke(ExecuteMethod executeMethod, Object self, Method method, Object... args) {
        Map<String, ?> commandArgs = ImmutableMap.of("using", "css selector", "value", args[0]);

        if ("findElementByCssSelector".equals(method.getName())) {
          return executeMethod.execute(DriverCommand.FIND_ELEMENT, commandArgs);
        } else if ("findElementsByCssSelector".equals(method.getName())) {
          return executeMethod.execute(DriverCommand.FIND_ELEMENTS, commandArgs);
        }

        throw new WebDriverException("Unmapped method: " + method.getName());
      }
    };
  }
}
