package workbook.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * A consumer that will run if more than waitTime milliseconds have passed since the
 * last call. The consumer will run when it is first called if immediate is set, otherwise
 * it will wait at least waitTime milliseconds before running.
 */
public class DebouncedConsumer<T> implements Consumer<T> {
	private final Object lock = new Object();
	
	private final Consumer<T> consumer;
	private final int waitTime;
	private final boolean immediate;
	
	private Timer timer = null;

	public DebouncedConsumer(int waitTime, boolean immediate, Consumer<T> consumer) {
		this.consumer = consumer;
		this.waitTime = waitTime;
		this.immediate = immediate;
	}
	
	public void accept(T param) {
		boolean shouldCall = false;
		
		synchronized(lock) {
			if(timer == null) {
				if(immediate) {
					shouldCall = true;
				}
			} else {
				timer.cancel();
				timer = null;
			}
			
			timer = new Timer(false);
			timer.schedule(new TimerTask() {
				public void run() {
					later(param);
				}
			}, waitTime);
		}
		
		if(shouldCall) {
			consumer.accept(param);
		}
	}

	private void later(T param) {
		synchronized(lock) {
			if(timer != null) {
				timer.cancel();
				timer = null;
			}
		}
		
		if(!immediate) {
			consumer.accept(param);
		}
	}
}