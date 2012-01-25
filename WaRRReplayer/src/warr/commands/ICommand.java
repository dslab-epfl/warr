package warr.commands;


public interface ICommand {
	boolean execute();

	String getID();

	String action();

	int getWaitTime();
}
