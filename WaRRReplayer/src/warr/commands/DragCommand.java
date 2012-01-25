package warr.commands;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RenderedRemoteWebElement;

public class DragCommand extends Command {

	private int x;
	private int y;

	public DragCommand(String id, int waitTime, int x, int y) {
		super(id, waitTime);
		this.x = x;
		this.y = y;
	}

	@Override
	protected boolean executeOn(WebElement element) throws Exception {
		if (element instanceof RenderedRemoteWebElement) {
			RenderedRemoteWebElement webElement = (RenderedRemoteWebElement) element;
			webElement.dragAndDropBy(x, y);
			return true;
		}
		return false;
	}

	@Override
	public String action() {
		return "drag";
	}
}
