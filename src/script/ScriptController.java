package script;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
	
	public <T> ScriptFuture<T> exec(Supplier<T> supplier) {
		ScriptFuture<T> future = new ScriptFuture<>();
		runnableQueue.add(() -> {
			try {
				future.complete(supplier.get());
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	public ScriptFuture<Object> eval(String expression, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		ScriptFuture<Object> future = new ScriptFuture<>();
		runnableQueue.add(() -> {
			try {
				Object result = script.eval(expression, outputCallback, errorCallback);
				future.complete(result);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	public ScriptFuture<Void> setVariable(String name, Object value) {
		ScriptFuture<Void> future = new ScriptFuture<>();
		runnableQueue.add(() -> {
			try {
				script.setVariable(name, value);
				future.complete(null);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	public void getScript(Consumer<Script> consumer) {
		runnableQueue.add(() -> {
			consumer.accept(script);
		});
	}
	
	public Script getScriptSync() {
		return script;
	}

	public void interrupt() {
		Thread thread = script.thread;
		if(thread != null && thread.isAlive()) {
			thread.interrupt();
		}
	}
}
