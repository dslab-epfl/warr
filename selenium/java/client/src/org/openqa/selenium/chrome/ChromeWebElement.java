package org.openqa.selenium.chrome;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.Point;
import org.openqa.selenium.RenderedWebElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RenderedRemoteWebElement;
import org.openqa.selenium.remote.Response;

public class ChromeWebElement extends RenderedRemoteWebElement
    implements Locatable {

  public ChromeWebElement(ChromeDriver parent) {
    setParent(parent);
  }
  
  @Override
  public void dragAndDropBy(int moveRightBy, int moveDownBy) {
    execute(DriverCommand.DRAG_ELEMENT, ImmutableMap.of("id", id, "x", moveRightBy, "y", moveDownBy));
  }

  @Override
  public void dragAndDropOn(RenderedWebElement element) {
    throw new UnsupportedOperationException("Not yet supported in Chrome");
  }

  public org.openqa.selenium.Point getLocationOnScreenOnceScrolledIntoView() {
    Response response = execute(DriverCommand.GET_ELEMENT_LOCATION_ONCE_SCROLLED_INTO_VIEW,
        ImmutableMap.of("id", getId()));
    @SuppressWarnings("unchecked")
    Map<String, Object> rawPoint = (Map<String, Object>) response.getValue();
    int x = ((Long) rawPoint.get("x")).intValue();
    int y = ((Long) rawPoint.get("y")).intValue();
    return new Point(x, y);
  }

  public Coordinates getCoordinates() {
    throw new UnsupportedOperationException("This chrome driver does not support advanced " 
        + "interactions.");
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WebElement)) {
      return false;
    }

    WebElement other = (WebElement) obj;
    if (other instanceof WrapsElement) {
      other = ((WrapsElement) obj).getWrappedElement();
    }

    if (!(other instanceof ChromeWebElement)) {
      return false;
    }

    return getId().equals(((ChromeWebElement)other).getId());
  }
}
