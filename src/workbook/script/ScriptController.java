package workbook.script;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages the interaction with the script engines. The script engine runs on a separate thread, and all interactions with it
 * must run on the same thread. This controller allows posting events to a queue so that they will run on the correct thread,
 * and return their results as a ScriptFuture.
 */
public class ScriptController {
	private final BlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<>();
	
	private final Map<String, Object> globals = new HashMap<>();
	private final Map<String, Engine> engines = new LinkedHashMap<>();

	private String scriptType;
	private Engine engine;
	
	private volatile Thread thread = null;
	
	/**
	 * Starts a thread to handle the items posted to the runnable queue.
	 */
	public void startQueueThread() {
		Thread thread = new Thread(this::runQueue);
		this.thread = thread;
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
	
	public String getScriptType() {
		return scriptType;
	}
	
	public ScriptFuture<Void> addEngine(String scriptType, Engine engine) {
		ScriptFuture<Void> future = new ScriptFuture<>(this);
		runnableQueue.add(() -> {
			try {
				engine.setGlobals(globals);
				engines.put(scriptType, engine);
				future.complete(null);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	public ScriptFuture<Void> setScriptType(String scriptType) {
		this.scriptType = scriptType;
		
		ScriptFuture<Void> future = new ScriptFuture<>(this);
		runnableQueue.add(() -> {
			try {
				engine = engines.get(scriptType);
				if(engine == null) {
					throw new IllegalArgumentException("Unknown script type: " + scriptType);
				}
				future.complete(null);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	public <T> ScriptFuture<T> exec(Supplier<T> supplier) {
		ScriptFuture<T> future = new ScriptFuture<>(this);
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
		ScriptFuture<Object> future = new ScriptFuture<>(this);
		runnableQueue.add(() -> {
			try {
				Object result = engine.eval(expression, outputCallback, errorCallback);
				future.complete(result);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	public ScriptFuture<List<NameAndProperties>> evalWithCallbackFunctions(String expression, List<String> callbackFunctionNames, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		ScriptFuture<List<NameAndProperties>> future = new ScriptFuture<>(this);
		runnableQueue.add(() -> {
			try {
				List<NameAndProperties> result = engine.evalWithCallbackFunctions(expression, callbackFunctionNames, outputCallback, errorCallback);
				future.complete(result);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	public ScriptFuture<Void> setVariable(String name, Object value) {
		ScriptFuture<Void> future = new ScriptFuture<>(this);
		runnableQueue.add(() -> {
			try {
				engine.setVariable(name, value);
				future.complete(null);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	public void getScript(Consumer<Engine> consumer) {
		runnableQueue.add(() -> {
			consumer.accept(engine);
		});
	}
	
	public Engine getScriptSync() {
		return engine;
	}

	public void interrupt() {
		if(thread != null && thread.isAlive()) {
			thread.interrupt();
		}
	}
}
