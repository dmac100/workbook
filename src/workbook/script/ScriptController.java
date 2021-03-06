package workbook.script;

import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;

import workbook.view.result.wrapper.ChartWrapper;

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

	private Consumer<String> outputCallback = line -> {};
	private Consumer<String> errorCallback = line -> {};
	
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
		redirectOutput(false);
		
		while(true) {
			try {
				runnableQueue.take().run();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Sends the system output and error streams to outputCallback and errorCallback consumers.
	 */
	private void redirectOutput(boolean currentThreadOnly) {
		LineReader outputReader = new LineReader(line -> outputCallback.accept(line));
    	LineReader errorReader = new LineReader(line -> errorCallback.accept(line));
    	if(currentThreadOnly) {
			System.setOut(new PrintStreamSplitter(Thread.currentThread(), new PrintStream(outputReader.getOutputStream()), System.out));
			System.setErr(new PrintStreamSplitter(Thread.currentThread(), new PrintStream(errorReader.getOutputStream()), System.err));
    	} else {
    		System.setOut(new PrintStream(outputReader.getOutputStream()));
	    	System.setErr(new PrintStream(errorReader.getOutputStream()));
    	}
	}

	public void setOutputCallbacks(Consumer<String> outputCallback, Consumer<String> errorCallback) {
		exec(() -> {
			this.outputCallback = outputCallback;
			this.errorCallback = errorCallback;
			return null;
		});
	}
	
	public String getScriptType() {
		return scriptType;
	}
	
	public ScriptFuture<Void> addEngine(String scriptType, Engine engine) {
		return exec(() -> {
			engine.setGlobals(globals);
			engines.put(scriptType, engine);
			return null;
		});
	}
	
	public ScriptFuture<Void> setScriptType(String scriptType) {
		this.scriptType = scriptType;
		return exec(() -> {
			for(String key:engines.keySet()) {
				if(key.equalsIgnoreCase(scriptType)) {
					engine = engines.get(key);
					if(engine == null) {
						throw new IllegalArgumentException("Unknown script type: " + scriptType);
					}
				}
			}
			return null;
		});
	}
	
	public <T> ScriptFuture<T> exec(Callable<T> callable) {
		ScriptFuture<T> future = new ScriptFuture<>(this);
		runnableQueue.add(() -> {
			try {
				future.complete(callable.call());
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	public ScriptFuture<Void> defineFunction(String name, Function<?, ?> callback) {
		return exec(() -> {
			engine.defineFunction(name, ChartWrapper::new);
			return null;
		});
	}
	
	public ScriptFuture<Object> evalMethodCall(String methodName, List<Object> params) {
		return exec(() -> {
			return engine.evalMethodCall(methodName, params);
		});
	}
	
	public ScriptFuture<Object> eval(String expression) {
		return exec(() -> {
			Object result = engine.eval(expression);
			engine.setVariable("_", result);
			return result;
		});
	}
	
	public ScriptFuture<List<NameAndProperties>> evalWithCallbackFunctions(String expression, List<String> callbackFunctionNames) {
		return exec(() -> {
			return engine.evalWithCallbackFunctions(expression, callbackFunctionNames);
		});
	}

	public ScriptFuture<Object> getVariable(String name) {
		return exec(() -> {
			return engine.getVariable(name);
		});
	}
	
	public ScriptFuture<Void> setVariable(String name, Object value) {
		return exec(() -> {
			engine.setVariable(name, value);
			return null;
		});
	}
	
	public ScriptFuture<Void> clearGlobals() {
		return exec(() -> {
			Object system = globals.get("system");
			globals.clear();
			globals.put("system", system);
			
			return null;
		});
	}
	
	public void getScript(Consumer<Engine> consumer) {
		runnableQueue.add(() -> {
			consumer.accept(engine);
		});
	}
	
	public Map<String, Object> getGlobalsSync() {
		return globals;
	}
	
	public Engine getScriptSync() {
		return engine;
	}

	public void interrupt() {
		if(thread != null && thread.isAlive()) {
			thread.interrupt();
		}
	}

	/**
	 * Returns the globals map serialized into a String.
	 */
	public ScriptFuture<String> serializeGlobals() {
		return exec(() -> {
			Map<String, Object> map = new HashMap<>(globals);
			map.remove("system");
			map.remove("_");
			return new ObjectSerializer().serialize(map);
		});
	}
	
	/**
	 * Deserializes the global map from a String.
	 */
	public ScriptFuture<Void> deserializeGlobals(String globalMap) {
		return exec(() -> {
			Map<String, Object> map = new ObjectSerializer().deserialize(globalMap);
			Object system = globals.get("system");
			globals.putAll(map);
			globals.put("system", system);
			return null;
		});
	}
}
