package script;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.Consumer;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class Script {
	private final ScriptEngine engine;
	
	volatile Thread thread = Thread.currentThread();
	
	public Script() {
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		engine = factory.getScriptEngine(new String[] { "--class-cache-size = 0" });
		if(engine == null) {
			throw new RuntimeException("Can't create JavaScript engine");
		}
		
		eval("function print() { System.out.println([].slice.call(arguments).join(', ')) }");
	}
	
	private void checkThreadAccess() {
		if(thread != Thread.currentThread()) {
			throw new RuntimeException("Invalid thread access: " + thread + " - " + Thread.currentThread().getName());
		}
	}
	
	public boolean isIterable(Object value) throws ScriptException {
		checkThreadAccess();
		
		Bindings bindings = engine.createBindings();
		bindings.put("arg", value);
		return (Boolean) engine.eval("Object.prototype.toString.call(arg) === '[object Array]'", bindings);
	}

	public void iterateObject(Object array, Consumer<Object> consumer) {
		checkThreadAccess();
		
		Map<?, ?> map = (Map<?, ?>) array;
		Long length = getNumeric(map.get("length"));
		if(length != null) {
			for(int i = 0; i < (Long) length; i++) {
				consumer.accept(map.get(String.valueOf(i)));
			}
		}
	}
	
	private static Long getNumeric(Object object) {
		if(object instanceof Integer) {
			return Long.valueOf((Integer) object);
		} else if(object instanceof Long) {
			return (Long) object;
		} else {
			return null;
		}
	}
	
	public void setVariable(String name, Object value) {
		checkThreadAccess();
		engine.put(name, value);
	}
	
	public Object getVariable(String name) {
		checkThreadAccess();
		return engine.get(name);
	}
	
	public Map<String, Object> getVariableMap() {
		checkThreadAccess();
		return engine.getBindings(ScriptContext.ENGINE_SCOPE);
	}
	
	public Object eval(String command) {
		checkThreadAccess();
		Consumer<String> nullCallback = x -> {};
		return eval(command, nullCallback, nullCallback);
	}
	
	public Object eval(String command, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		checkThreadAccess();
        PrintStream out = System.out;
        PrintStream err = System.err;
        try {
        	LineReader outputReader = new LineReader(outputCallback);
        	LineReader errorReader = new LineReader(errorCallback);
        	
        	System.setOut(new PrintStreamSplitter(Thread.currentThread(), new PrintStream(outputReader.getOutputStream()), out));
        	System.setErr(new PrintStreamSplitter(Thread.currentThread(), new PrintStream(errorReader.getOutputStream()), err));
	        
			Object value = engine.eval("with(new JavaImporter(java.util, java.lang)) { " + command + "}");

			System.out.close();
			System.err.close();
			
			outputReader.waitUntilDone();
			errorReader.waitUntilDone();
			
			return value;
        } catch(ScriptException e) {
        	e.printStackTrace(err);
        	errorCallback.accept(e.getMessage());
        	return null;
        } finally {
        	System.setOut(out);
        	System.setErr(err);
        }
	}
}