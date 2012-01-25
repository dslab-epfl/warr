package warr.commands;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import warr.WaRRReplayer;

public class DoubleClick extends ClickCommand {

	public DoubleClick(String id, int time, int x, int y) {
		super(id, time, x, y);
	}

	@Override
	protected boolean executeOn(WebElement element) {
		int tries = 3;
		while (tries-- > 0) {
			try {
				((JavascriptExecutor) WaRRReplayer.driver)
						.executeScript(
								"var evt = document.createEvent('MouseEvents');"
										+ "evt.initMouseEvent('dblclick',true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0,null);"
										+ "arguments[0].dispatchEvent(evt);",
								element);
				return true;
			} catch (Exception e) {
				System.out.println("Clicking failed: " + e);
				System.out.println("Trying again:");
			}
		}
		return false;
	}

	@Override
	public String action() {
		return "doubleclick";
	}
}
