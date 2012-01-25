package warr.commands;

import org.openqa.selenium.JavascriptExecutor;

import warr.WaRRReplayer;

public class Assert implements ICommand {
	private String textToFind;
	private int waitTime;

	public Assert(String text, int time) {
		textToFind = text;
		waitTime = time;
	}

	@Override
	public boolean execute() {
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return (Boolean) ((JavascriptExecutor) WaRRReplayer.driver)
				.executeScript("return window.find('" + textToFind + "')",
						new Object[0]);

	}

	@Override
	public String getID() {
		return textToFind;
	}

	@Override
	public String action() {
		return "assert";
	}

	@Override
	public int getWaitTime() {
		return waitTime;
	}

	public String toString() {
		return action() + " " + textToFind;
	}

	public String getText() {
		return textToFind;
	}

}
