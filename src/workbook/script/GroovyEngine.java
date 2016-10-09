package workbook.script;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class GroovyEngine implements Engine {
	private final ScriptEngine engine;
	private final Map<String, Object> globals;
	
	public GroovyEngine(Map<String, Object> globals) {
		this.globals = globals;
		
		engine = new ScriptEngineManager().getEngineByName("groovy");
		if(engine == null) {
			throw new RuntimeException("Can't create Groovy engine");
		}
	}
	
	public boolean isIterable(Object value) {
		return (value instanceof Iterable);
	}

	public void iterateObject(Object array, Consumer<Object> consumer) {
		Iterable iterable = (Iterable) array;
		for(Object value:iterable) {
			consumer.accept(value);
		}
	}
	
	public void setVariable(String name, Object value) {
		engine.put(name, value);
		globals.put(name, value);
	}
	
	public Object getVariable(String name) {
		return engine.get(name);
	}
	
	public boolean isScriptObject(Object object) {
		return (object instanceof Map);
	}

	public Map<Object, Object> getPropertyMap(Object object) {
		if(object instanceof Map) {
			return (Map<Object, Object>) object;
		}
		
		return new HashMap<>();
	}
	
	public Object eval(String command) {
		Consumer<String> nullCallback = x -> {};
		return eval(command, nullCallback, nullCallback);
	}
	
	/**
	 * Evaluates a command, and returns the result.
	 */
	public Object eval(String command, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		return eval(command, "", null, outputCallback, errorCallback);
	}

	/**
	 * Evaluates a command against a list of callback functions and returns the functions that were called. So if callbackFunctionNames contains
	 * 'rect', and command contains the function call 'rect({x: 1})', then [NameAndProperties('rect', { x => 1 })] will be returned.
	 */
	public List<NameAndProperties> evalWithCallbackFunctions(String command, List<String> callbackFunctionNames, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		List<NameAndProperties> callbackValues = new ArrayList<>();
		
		Bindings bindings = engine.createBindings();
		bindings.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));
		
		bindings.put("callback", new BiConsumer<String, Map<Object, String>>() {
			public void accept(String name, Map<Object, String> properties) {
				Map<String, String> map = new HashMap<>();
				
				for(Object key:new HashSet<>(properties.keySet())) {
					map.put(String.valueOf(key), String.valueOf(properties.get(key)));
				}
				
				NameAndProperties nameAndProperties = new NameAndProperties(name, map);
				callbackValues.add(nameAndProperties);
			}
		});
		
		StringBuilder prefix = new StringBuilder();
		for(String name:callbackFunctionNames) {
			prefix.append(String.format("void %s(values) { callback.accept('%s', new java.util.HashMap(values)); }", name, name));
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
        	
        	PrintWriter outputWriter = new PrintWriter(outputReader.getOutputStream());
        	PrintWriter errorWriter = new PrintWriter(errorReader.getOutputStream());

        	engine.getContext().setWriter(outputWriter);
        	engine.getContext().setErrorWriter(errorWriter);
        	
        	engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(globals);
        	
        	String script = String.format("%s; %s;", prefix, command);
			Object value = (bindings == null) ? engine.eval(script) : engine.eval(script, bindings);
			
			globals.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));
			
			outputWriter.close();
			errorWriter.close();
			
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