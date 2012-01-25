package warr.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandFactory {

	public static int waitTime = 10;
	private static final Pattern typePatternWithTime = Pattern
			.compile("type\\s+\\[([^?]+),\\s*(\\d+)\\]\\s+(.*)\\s+(\\d+)");

	private static final Pattern typePattern = Pattern
			.compile("type\\s+\\[(.),\\s*(\\d+)\\]\\s+(.*)");
	private static final Pattern dragPatternWithTime = Pattern
			.compile("drag\\s+(.+)\\s+(\\d+)\\s+(-*\\d+),(-*\\d+)");
	private static final Pattern dragPattern = Pattern
			.compile("drag\\s+(.+)\\s+(-*\\d+),(-*\\d+)");
	private static final Pattern clickPattern = Pattern
			.compile("(\\w+)\\s+(.+)\\s+(\\d+)\\D+(\\d+)");
	private static final Pattern assertPattern = Pattern
			.compile("assert\\s+(.+)");
	private static final Pattern assertPatternWithTime = Pattern
			.compile("assert\\s+(.+)\\D+(\\d+)");
	private static final Pattern clickPatternWithTime = Pattern
			.compile("(\\w+)\\s+(.+)\\s+(\\d+)\\s+(\\d+)\\D+(\\d+)");

	private static TypeCommand getTypeCommand(Matcher m, int time) {
		String text;
		if (m.group(1).equals("?")) {
			text = new String(
					new char[] { (char) Integer.parseInt(m.group(2)) });
		} else {
			text = m.group(1);
		}
		return new TypeCommand(m.group(3), time, text);
	}

	public static ICommand getCommand(String command) {
		command = command.trim();
		if (command.startsWith("click ")) {
			Matcher m = clickPatternWithTime.matcher(command);
			if (m.matches()) {
				return new ClickCommand(m.group(2),
						Integer.valueOf(m.group(3)),
						Integer.valueOf(m.group(4)),
						Integer.valueOf(m.group(5)));
			}
			m = clickPattern.matcher(command);
			if (m.matches()) {
				return new ClickCommand(m.group(2), waitTime, Integer.valueOf(m
						.group(3)), Integer.valueOf(m.group(4)));
			}
		}

		if (command.startsWith("doubleclick ")) {
			Matcher m = clickPatternWithTime.matcher(command);
			if (m.matches()) {
				return new DoubleClick(m.group(2), Integer.valueOf(m.group(3)),
						Integer.valueOf(m.group(4)),
						Integer.valueOf(m.group(5)));
			}

			m = clickPattern.matcher(command);
			if (m.matches()) {
				return new DoubleClick(m.group(2), waitTime, Integer.valueOf(m
						.group(3)), Integer.valueOf(m.group(4)));
			}
		}

		if (command.startsWith("drag")) {
			Matcher m = dragPatternWithTime.matcher(command);
			if (m.matches()) {
				return new DragCommand(m.group(1), Integer.valueOf(m.group(2)), Integer.valueOf(m
						.group(3)), Integer.valueOf(m.group(4)));
			}
			
			m = dragPattern.matcher(command);
			if(m.matches()){
				return new DragCommand(m.group(1), waitTime, Integer.valueOf(m
					.group(2)), Integer.valueOf(m.group(3)));
			}
		}
		if (command.startsWith("open ")) {
			return new OpenCommand(command.split("\\s+")[1]);
		}
		if (command.startsWith("type ")) {
			Matcher m = typePatternWithTime.matcher(command);
			if (m.matches()) {
				return getTypeCommand(m, Integer.valueOf(m.group(4)));
			}

			m = typePattern.matcher(command);
			if (m.matches()) {
				return getTypeCommand(m, waitTime);
			}
		}
		if (command.startsWith("assert")) {
			Matcher m = assertPatternWithTime.matcher(command);
			if (m.matches()) {
				return new Assert(m.group(1), Integer.valueOf(m.group(2)));
			}
			m = assertPattern.matcher(command);
			if (m.matches()) {
				return new Assert(m.group(1), waitTime);
			}
		}
		return null;
	}

	public static List<ICommand> getCommands(InputStream input) {
		List<ICommand> result = new Vector<ICommand>();
		BufferedReader br = new BufferedReader(new InputStreamReader(input));
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				ICommand command = getCommand(line);
				System.out.println("READ: " + command);
				if (command == null) {
					System.out.println("NOT ADDING");
					continue;
				}
				result.add(command);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
