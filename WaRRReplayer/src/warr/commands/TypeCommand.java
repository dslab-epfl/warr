package warr.commands;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

public class TypeCommand extends Command {

	private String text;

	public TypeCommand(String id, int waitTime, String text) {
		super(id, waitTime);
		this.text = text;
	}

	protected String postString() {
		return text;
	}

	@Override
	protected boolean executeOn(WebElement element) {
		if (text.getBytes()[0] == 13) {
			System.err.println("SENDING ENTER");
			element.sendKeys(Keys.RETURN);
		}
		element.sendKeys(text);
		return true;
	}

	@Override
	public String action() {
		return "type";
	}

	public String text() {
		return text;
	}

}
