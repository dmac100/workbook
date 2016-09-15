package script;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class ScriptController {
	private final BlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<>();
	private final Script script = new Script();
	
	/**
	 * Starts a thread to handle the items posted to the runnable queue.
	 */
	public void startQueueThread() {
		Thread thread = new Thread(this::runQueue);
		script.thread = thread;
		thread.setDaemon(true);
		thread.setName("Script Thread");
		thread.start();

		// Restart thread on exception.
		thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				e.printStackTrace();
				startQueueThread();
			}
		});
	}
	
	private void runQueue() {
		while(true) {
			try {
				runnableQueue.take().run();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public ScriptFuture<Object> eval(String expression, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		ScriptFuture<Object> future = new ScriptFuture<>();
		runnableQueue.add(() -> {
			Object result = script.eval(expression, outputCallback, errorCallback);
			future.complete(result);
		});
		return future;
	}

	public ScriptFuture<Void> setVariable(String name, Object value) {
		ScriptFuture<Void> future = new ScriptFuture<>();
		runnableQueue.add(() -> {
			script.setVariable(name, value);
			future.complete(null);
		});
		return future;
	}
	
	public void getScript(Consumer<Script> consumer) {
		runnableQueue.add(() -> {
			consumer.accept(script);
		});
	}

	public void interrupt() {
		Thread thread = script.thread;
		if(thread != null && thread.isAlive()) {
			thread.interrupt();
		}
	}
}
