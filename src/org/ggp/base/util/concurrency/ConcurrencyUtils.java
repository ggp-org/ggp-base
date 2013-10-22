package org.ggp.base.util.concurrency;

public class ConcurrencyUtils {
	/**
	 * If the thread has been interrupted, throws an InterruptedException.
	 */
	public static void checkForInterruption() throws InterruptedException {
		if (Thread.currentThread().isInterrupted())
			throw new InterruptedException();
	}
}
