package org.openqa.selenium.server;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BrowserResponseSequencer {
	static Logger log = Logger.getLogger(BrowserResponseSequencer.class.getName());
  int num = 0;
	final Lock lock;
	final Condition numIncreased;
	final String uniqueId;
	public BrowserResponseSequencer(String uniqueId) {
		this.uniqueId = uniqueId;
		this.lock = new ReentrantLock();
		numIncreased = lock.newCondition();
	}
	
	public void increaseNum() {
		lock.lock();
		try {
			num++;
			numIncreased.signalAll();
		} finally {
			lock.unlock();
		}
		
	}
	
	public void waitUntilNumIsAtLeast(int expected) {
		lock.lock();
		try {
			while(true) {
				if (num >= expected) return;
				log.fine("Waiting "+uniqueId+", expected sequence number " + expected + ", was " + num + ".");
				boolean timedOut = false;
				try {
					timedOut = !numIncreased.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					log.log(Level.FINE, "interrupted", e);
				}
				if (timedOut) {
					log.warning(uniqueId + " expected sequence number " + expected + ", was " + num + ".  Continuing anyway");
					num++;
					numIncreased.signalAll();
				}
			}
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public String toString() {
		return uniqueId + ": " + num;
	}
}
