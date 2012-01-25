package warr.commands;

import java.util.List;
import java.util.Vector;

import warr.WaRRReplayer;

public class CommandExecutor {

	public static boolean CLEAR_COOKIES;
	public static int ID;

	public static boolean ABANDON = false;

	public static List<ICommand> execute(List<ICommand> commands) {
		List<ICommand> successfullyExecutedCommands = new Vector<ICommand>();
		try {
			for (ICommand command : commands) {
				try {
					System.out
							.println("==================================\nexecuting "
									+ command);
					if (!command.execute()) {
						if (ABANDON) {
							return successfullyExecutedCommands;
						}
					}
					successfullyExecutedCommands.add(command);
				} catch (Exception e) {
					e.printStackTrace();
					return successfullyExecutedCommands;
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			return successfullyExecutedCommands;
		} finally {
			if (CLEAR_COOKIES) {
				System.out.println("CLEARING COOKIES");
				WaRRReplayer.driver.manage().deleteAllCookies();
			}
		}
		return successfullyExecutedCommands;
	}
}
