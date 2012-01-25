package warr.commands;

import org.openqa.selenium.WebElement;

public class ClickCommand extends Command {
	private int x;
	private int y;

	public ClickCommand(String id, int waitTime, int x, int y) {
		super(id, waitTime);
		this.x = x;
		this.y = y;
	}

	@Override
	protected boolean executeOn(WebElement element) {
		element.click();
		return true;
	}

	public ICommand cloneCommand() {
		return new ClickCommand(getID(), getWaitTime(), x, y);
	}

	@Override
	public String action() {
		return "click";
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
}
