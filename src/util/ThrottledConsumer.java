package util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * A consumer that runs at most every waitTime milliseconds. The consumer
 * will run when it is first called, and again if called after waitTime milliseconds
 * have passed. If trailing is set then it will run any missed calls after waitTime
 * milliseconds have passed.
 */
public class ThrottledConsumer<T> implements Consumer<T> {
	private final Object lock = new Object();
	
	private final Consumer<T> consumer;
	private final int waitTime;
	private final boolean trailing;
	
	private Timer timer = null;
	private long lastTime = 0;

	public ThrottledConsumer(int waitTime, boolean trailing, Consumer<T> consumer) {
		this.consumer = consumer;
		this.waitTime = waitTime;
		this.trailing = trailing;
	}
	
	public void accept(T param) {
		boolean shouldCall = false;
		
		synchronized(lock) {
			long elapsedTime = System.currentTimeMillis() - lastTime;
			
			if(timer == null && elapsedTime >= waitTime) {
				shouldCall = true;
				lastTime = System.currentTimeMillis();
			} else if(trailing) {
				if(timer == null) {
					timer = new Timer(false);
					timer.schedule(new TimerTask() {
						public void run() {
							later(param);
						}
					}, Math.max(0, waitTime - elapsedTime));
				}
			}
		}
		
		if(shouldCall) {
			consumer.accept(param);
		}
	}
	
	private void later(T param) {
		synchronized(lock) {
			if(timer != null) {
				lastTime = System.currentTimeMillis();
				timer.cancel();
				timer = null;
			}
		}
		
		consumer.accept(param);
	}
}