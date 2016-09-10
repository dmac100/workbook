package script;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ScriptController {
	private final ArrayBlockingQueue<Runnable> runnableQueue = new ArrayBlockingQueue<>(50);
	private final Script script = new Script();
	
	/**
	 * Starts a thread to handle the items posted to the runnable queue.
	 */
	public void startQueueThread() {
		Thread thread = new Thread(this::runQueue);
		thread.setDaemon(true);
		thread.setName("Script Thread");
		thread.start();

		// Restart thread on exception.
		thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				runnableQueue.clear();
				
				e.printStackTrace();
				startQueueThread();
			}
		});
	}
	
	private void runQueue() {
		try {
			while(true) {
				runnableQueue.take().run();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public CompletableFuture<Object> eval(String expression, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		CompletableFuture<Object> future = new CompletableFuture<>();
		runnableQueue.add(() -> {
			Object result = script.eval(expression, outputCallback, errorCallback);
			future.complete(result);
		});
		return future;
	}

	public CompletableFuture<Void> setVariable(String name, Object value) {
		CompletableFuture<Void> future = new CompletableFuture<>();
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
}
