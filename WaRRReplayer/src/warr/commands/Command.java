package warr.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import warr.WaRRReplayer;

public abstract class Command implements ICommand {
	private static Map<String, String> mapFromSearchedToResolved = new HashMap<String, String>();
	private static List<String> failed = new Vector<String>();
	private static Map<String, String> mapFromSearchedToFrame = new HashMap<String, String>();
	private String id;
	private int waitTime;
	private WebElement target;
	private static boolean firstTime = true;

	public Command(String id, int waitTime) {
		super();
		this.id = id;
		this.waitTime = waitTime;
	}

	private static WebElement lastElement;

	protected static void setLastElement(WebElement last) {
		lastElement = last;
		firstTime = true;
	}

	protected static WebElement lastElement() {
		return lastElement;
	}

	public Command(String id) {
		super();
		this.id = id;
		waitTime = 0;
	}

	@Override
	public boolean execute() {
		try {
			if (waitTime > 0) {
				Thread.sleep(waitTime);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		target = getWebElement();
		return executeTheThing();
	}

	protected static boolean firstTime() {
		boolean res = firstTime;
		firstTime = false;
		return res;
	}

	@Override
	public String getID() {
		return id;
	}

	public String toString() {
		return action() + " " + id + " " + waitTime + " " + postString();
	}

	protected String postString() {
		return "";
	}

	protected abstract boolean executeOn(WebElement element) throws Exception;

	private boolean executeTheThing() {
		try {
			if (target == null) {
				return false;
			}
			if (!executeOn(target)) {
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private static WebElement findWebElement(String id, WebDriver driver) {
		WebElement element = null;
		if (id.startsWith("//")) {
			element = driver.findElement(By.xpath(id));
		} else {
			element = driver.findElement(By.id(id));
		}
		return element;
	}

	public static WebElement findWebElement(String id) {
		return findWebElement(id, WaRRReplayer.driver);
	}

	private static WebElement doTheTries(String id, WebDriver driver) {
		int tries = 1;
		while (tries-- > 0) {
			try {
				WebElement we = findWebElement(id, driver);
				return we;
			} catch (Exception e) {
				System.out.println("Could not find " + id + " because of " + e);
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static String keepOnlyProperty(String initial, String property) {
		int index = property.equals("text()") ? initial.indexOf(property)
				: initial.indexOf("@" + property);
		if (index == -1)
			return null;
		String newString = initial.substring(index);
		int end = newString.indexOf('"', newString.indexOf('"') + 1);
		return newString.substring(0, end + 1);

	}

	private static String keepProperties(String xpath, String[] property) {
		int index = xpath.indexOf('[');
		int prevIndex = 0;
		String result = "";
		while (index > 0) {
			result += xpath.substring(prevIndex, index);
			int closingBrace = xpath.indexOf(']', index) + 1;
			String subString = xpath.substring(index, closingBrace);
			String toAdd = "";
			int count = 0;
			for (int i = 0; i < property.length; i++) {
				String removed = keepOnlyProperty(subString, property[i]);
				if (removed == null) {
					continue;
				}

				if (count != 0) {
					toAdd += " and ";
				}
				toAdd += removed;
				count++;
			}
			result += '[' + toAdd + ']';
			prevIndex = closingBrace;
			index = xpath.indexOf('[', prevIndex);
		}
		return (result + xpath.substring(prevIndex)).replaceAll("\\[\\]", "")
				.trim();
	}

	private static String removeProperty(String xpath, String property) {
		String newXPath = xpath.replaceAll(property + "=\"[^\"]*\"\\s*", "");
		newXPath = newXPath.replaceAll("\\[\\]", "").replace("and and", "and")
				.replaceAll("\\[\\s*and", "[").replaceAll("and\\s*]", "]");
		return newXPath.trim();

	}

	private static WebElement findElement(String id, WebDriver driver) {
		WebElement target;
		target = doTheTries(id, driver);
		if (target != null) {
			return target;
		}

		String newID = keepProperties(id, new String[] { "name" });
		if (!newID.equals(id)) {
			target = doTheTries(newID, driver);
			if (target != null) {
				mapFromSearchedToResolved.put(id, newID);
				return target;
			}
		}

		newID = keepProperties(id, new String[] { "id" });
		if (!newID.equals(id)) {
			target = doTheTries(newID, driver);
			if (target != null) {
				mapFromSearchedToResolved.put(id, newID);
				return target;
			}
		}

		newID = removeProperty(id, "@id");
		if (!newID.equals(id)) {
			target = doTheTries(newID, driver);
			if (target != null) {
				mapFromSearchedToResolved.put(id, newID);
				return target;
			}
		}

		newID = removeProperty(id, "text\\(\\)");
		if (!newID.equals(id)) {
			target = doTheTries(newID, driver);
			if (target != null) {
				mapFromSearchedToResolved.put(id, newID);
				return target;
			}
		}
		newID = removeProperty(id, "@href");
		if (!newID.equals(id)) {
			target = doTheTries(newID, driver);
			if (target != null) {
				mapFromSearchedToResolved.put(id, newID);
				return target;
			}
		}
		// then the name
		newID = removeProperty(id, "@name");
		if (!newID.equals(id)) {
			target = doTheTries(newID, driver);
			if (target != null) {
				mapFromSearchedToResolved.put(id, newID);
				return target;
			}
		}

		newID = keepProperties(id, new String[] { "id", "name", "title",
				"text()" });
		if (!newID.equals(id)) {
			target = doTheTries(newID, driver);
			if (target != null) {
				mapFromSearchedToResolved.put(id, newID);
				return target;
			}
		}

		newID = keepProperties(id, new String[] { "title" });
		if (!newID.equals(id)) {
			target = doTheTries(newID, driver);
			if (target != null) {
				mapFromSearchedToResolved.put(id, newID);
				return target;
			}
		}
		newID = keepProperties(id, new String[] { "text()" });
		if (!newID.equals(id)) {
			target = doTheTries(newID, driver);
			if (target != null) {
				mapFromSearchedToResolved.put(id, newID);
				return target;
			}
		}

		newID = removeFirstComponentOfXPath(id);
		if (!newID.equals(id)) {
			WebElement webElement = findElement(newID, driver);
			mapFromSearchedToResolved.put(id, newID);
			return webElement;
		}
		return target;
	}

	private static Pattern patternForXPath = Pattern
			.compile("/(/[a-zA-Z]+(\\[.*\\])?)(/[a-zA-Z]+(\\[.*\\])?)");

	private static String removeFirstComponentOfXPath(String id) {
		Matcher m = patternForXPath.matcher(id);
		if (m.matches()) {
			return "/" + m.group(3);
		}
		return id;
	}

	private static interface Strategy {
		WebElement searchHere(WebDriver driver, String id);
	}

	private static class ElementFinder implements Strategy {

		@Override
		public WebElement searchHere(WebDriver driver, String id) {
			return findElement(id, driver);
		}

	}

	private static class FrameFinder implements Strategy {
		@Override
		public WebElement searchHere(WebDriver driver, String id) {
			if (id.equals(currentFrame)) {
				return driver.switchTo().activeElement();
			}
			int frameIndex = id.toUpperCase().indexOf("/IFRAME");
			int endIndex = id.indexOf(']', frameIndex + 1);
			String frame = "";
			if (endIndex != -1) {
				frame = id.substring(frameIndex, endIndex);
			}
			try {
				String name = keepOnlyProperty(frame, "name").replace("@name=",
						"").replaceAll("\"", "");
				driver = driver.switchTo().frame(name);
				currentFrame = id;
				return driver.switchTo().activeElement();
			} catch (Throwable e) {
			}
			try {
				String name = keepOnlyProperty(frame, "id").replace("@id=", "")
						.replaceAll("\"", "");
				driver = driver.switchTo().frame(name);
				currentFrame = id;
				return driver.switchTo().activeElement();
			} catch (Throwable t) {
			}

			WebElement target = findElement(id, driver);
			if (target != null) {
				driver.switchTo().frame(target);
				currentFrame = id;
				return target;
			}
			return null;
		}
	}

	private static WebElement searchForElementRecursively(WebDriver driver,
			List<WebElement> frames, String id, Strategy strategy) {
		id = id.trim();
		WebElement target = strategy.searchHere(driver, id);
		if (target != null) {
			return target;
		}
		driver = driver.switchTo().frame("WaRRResetFrame");
		for (WebElement o : frames) {
			driver = driver.switchTo().frame(o);
		}
		target = strategy.searchHere(driver, id);
		if (target != null) {
			return target;
		}
		for (WebElement we : driver.findElements(By.tagName("iframe"))) {
			Vector<WebElement> newList = new Vector<WebElement>(frames);
			newList.add(we);
			WebElement res = searchForElementRecursively(driver, frames, id,
					strategy);
			if (res != null) {
				return res;
			}
		}
		return null;
	}

	private static String currentFrame = "";

	private String getResolvedID(String id) {
		if (mapFromSearchedToResolved.containsKey(id)) {
			return getResolvedID(mapFromSearchedToResolved.get(id));
		}
		return id;
	}

	public WebElement getWebElement() {
		if (target != null) {
			return target;
		}
		id = getResolvedID(id);

		if (mapFromSearchedToFrame.containsKey(id)) {
			WaRRReplayer.driver.switchTo()
					.frame(mapFromSearchedToFrame.get(id));
			target = WaRRReplayer.driver.switchTo().activeElement();
			return target;
		}
		if (failed.contains(id)) {
			target = WaRRReplayer.driver.switchTo().activeElement();
			return target;
		}

		WebDriver driver = WaRRReplayer.driver;
		String id = this.id;
		if (id.contains("//INPUT") || id.contains("//input")) {
			int index = id.lastIndexOf('/');
			if (index >= 2) {
				id = id.substring(0, index);
			} else {
				System.out.println("OK");
			}
		}

		if (id.toUpperCase().contains("/IFRAME")) {
			return searchForElementRecursively(driver,
					new Vector<WebElement>(), id, new FrameFinder());
		}
		return searchForElementRecursively(driver, new Vector<WebElement>(),
				id, new ElementFinder());
	}

	public int getWaitTime() {
		return waitTime;
	}
}
