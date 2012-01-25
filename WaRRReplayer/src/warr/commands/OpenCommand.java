package warr.commands;

import warr.WaRRReplayer;

public class OpenCommand implements ICommand {

	private String url;

	public OpenCommand(String url) {
		this.url = url;
	}

	public String toString() {
		return "open " + url;
	}

	@Override
	public boolean execute() {
		WaRRReplayer.initDriver();
		WaRRReplayer.driver.get(url);
		return true;
	}

	@Override
	public String getID() {
		return url;
	}

	@Override
	public String action() {
		return "open";
	}

	@Override
	public int getWaitTime() {
		return 0;
	}
}
