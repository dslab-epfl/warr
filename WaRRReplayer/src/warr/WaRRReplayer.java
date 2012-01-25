package warr;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import warr.commands.CommandExecutor;
import warr.commands.CommandFactory;

public class WaRRReplayer {
	public static volatile WebDriver driver;

	public static void initDriver() {
		try {
			if (driver != null)
				return;
			driver = new ChromeDriver();
			driver.manage().timeouts()
					.implicitlyWait(10000, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void exit() {
		driver.quit();
		driver = null;
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out
					.println("to replay a WaRR-recorded trace, execute\njava -Dwebdriver.chrome.bin=<path to WaRR-enabled chrome> warr.WaRRReplayer <file.warr>");
			System.out.println(1);
		}
		System.out.println("Will replay: " + args[0]);
		try {
			initDriver();
			CommandExecutor.execute(CommandFactory
					.getCommands(new FileInputStream(args[0])));
			exit();
			System.out.println("Successfully completed the replay");
		} catch (FileNotFoundException e) {
			System.err.println("Replay failed");
			e.printStackTrace();
		}

	}
}
