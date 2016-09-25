package script;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JavascriptEngine implements Engine {
	private final ScriptEngine engine;
	
	public JavascriptEngine() {
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		engine = factory.getScriptEngine(new String[] { "--class-cache-size = 0" });
		if(engine == null) {
			throw new RuntimeException("Can't create JavaScript engine");
		}
		
		eval("function print() { System.out.println([].slice.call(arguments).join(', ')) }");
	}
	
	public boolean isIterable(Object value) throws ScriptException {
		Bindings bindings = engine.createBindings();
		bindings.put("arg", value);
		return (Boolean) engine.eval("Object.prototype.toString.call(arg) === '[object Array]'", bindings);
	}

	public void iterateObject(Object array, Consumer<Object> consumer) {
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
		engine.put(name, value);
	}
	
	public Object getVariable(String name) {
		return engine.get(name);
	}
	
	public Map<String, Object> getVariableMap() {
		return engine.getBindings(ScriptContext.ENGINE_SCOPE);
	}

	public boolean isScriptObject(Object object) {
		return object instanceof ScriptObjectMirror;
	}

	public Map<Object, Object> getPropertyMap(Object object) {
		return (Map<Object, Object>) object;
	}
	
	public Object eval(String command) {
		Consumer<String> nullCallback = x -> {};
		return eval(command, nullCallback, nullCallback);
	}
	
	/**
	 * Evaluates a command, and returns the result.
	 */
	public Object eval(String command, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		return eval(command, null, null, outputCallback, errorCallback);
	}

	/**
	 * Evaluates a command against a list of callback functions and returns the functions that were called. So if callbackFunctionNames contains
	 * 'rect', and command contains the function call 'rect({x: 1})', then [NameAndProperties('rect', { x => 1 })] will be returned.
	 */
	public List<NameAndProperties> evalWithCallbackFunctions(String command, List<String> callbackFunctionNames, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		List<NameAndProperties> callbackValues = new ArrayList<>();
		
		Bindings bindings = engine.createBindings();
		bindings.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));
		
		bindings.put("callback", new BiConsumer<String, Map<String, String>>() {
			public void accept(String name, Map<String, String> properties) {
				for(String key:new HashSet<>(properties.keySet())) {
					properties.put(key, String.valueOf(properties.get(key)));
				}
				
				NameAndProperties nameAndProperties = new NameAndProperties(name, properties);
				callbackValues.add(nameAndProperties);
			}
		});
		
		StringBuilder prefix = new StringBuilder();
		for(String name:callbackFunctionNames) {
			prefix.append(String.format("function %s(values) { callback.accept('%s', new java.util.HashMap(values)); }", name, name));
			prefix.append("\n");
		}
		
		eval(command, prefix.toString(), bindings, outputCallback, errorCallback);
		
		return callbackValues;
	}
	
	private Object eval(String command, String prefix, Bindings bindings, Consumer<String> outputCallback, Consumer<String> errorCallback) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        try {
        	LineReader outputReader = new LineReader(outputCallback);
        	LineReader errorReader = new LineReader(errorCallback);
        	
        	System.setOut(new PrintStreamSplitter(Thread.currentThread(), new PrintStream(outputReader.getOutputStream()), out));
        	System.setErr(new PrintStreamSplitter(Thread.currentThread(), new PrintStream(errorReader.getOutputStream()), err));
        	
        	String script = String.format("%s; with(new JavaImporter(java.util, java.lang)) { %s; }", prefix, command);
			Object value = (bindings == null) ? engine.eval(script) : engine.eval(script, bindings);
			
			System.out.close();
			System.err.close();
			
			outputReader.waitUntilDone();
			errorReader.waitUntilDone();
			
			return value;
        } catch(Exception e) {
        	e.printStackTrace(err);
        	errorCallback.accept(e.getMessage());
        	return null;
        } finally {
        	System.setOut(out);
        	System.setErr(err);
        }
	}
}