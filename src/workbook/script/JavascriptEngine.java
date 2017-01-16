package workbook.script;

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
import javax.script.ScriptException;

import com.google.common.base.Throwables;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import syntaxhighlighter.brush.Brush;
import syntaxhighlighter.brush.BrushJScript;

/**
 * An engine using the JavaScript scripting language.
 */
public class JavascriptEngine implements Engine {
	private final ScriptEngine engine;
	private Map<String, Object> globals = new HashMap<>();
	
	public JavascriptEngine() {
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		engine = factory.getScriptEngine(new String[] { "--class-cache-size = 0" });
		if(engine == null) {
			throw new RuntimeException("Can't create JavaScript engine");
		}
		
		eval("function print() { System.out.println([].slice.call(arguments).join(', ')) }");
	}
	
	public Brush getBrush() {
		return new BrushJScript();
	}
	
	public void setGlobals(Map<String, Object> globals) {
		this.globals = globals;
	}
	
	public boolean isIterable(Object value) {
		try {
			Bindings bindings = engine.createBindings();
			bindings.put("arg", value);
			return (Boolean) engine.eval("Object.prototype.toString.call(arg) === '[object Array]'", bindings);
		} catch(ScriptException e) {
			throw new RuntimeException("Error checking iterable", e);
		}
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
		globals.put(name, value);
	}
	
	public Object getVariable(String name) {
		return engine.get(name);
	}
	
	public boolean isScriptObject(Object object) {
		return object instanceof ScriptObjectMirror;
	}

	public Map<Object, Object> getPropertyMap(Object object) {
		return (Map<Object, Object>) object;
	}
	
	/**
	 * Evaluates a method given its name and list of parameters, and returns the result.
	 */
	public Object evalMethodCall(String methodName, List<Object> params) {
		Bindings bindings = engine.createBindings();
		bindings.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));
		bindings.put("arguments", params);
		// TODO: Apply only works for javascript functions, not Java methods.
		String command = "(" + methodName + ").apply(null, arguments)";
		return eval(command, bindings);
	}
	
	/**
	 * Evaluates a command, and returns the result.
	 */
	public Object eval(String command) {
		return eval(command, null);
	}

	/**
	 * Evaluates a command against a list of callback functions and returns the functions that were called. So if callbackFunctionNames contains
	 * 'rect', and command contains the function call 'rect({x: 1})', then [NameAndProperties('rect', { x => 1 })] will be returned.
	 */
	public List<NameAndProperties> evalWithCallbackFunctions(String command, List<String> callbackFunctionNames) {
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
		
		eval(prefix.toString(), bindings);
		eval(command, bindings);
		
		return callbackValues;
	}
	
	private Object eval(String command, Bindings bindings) {
        try {
        	engine.getContext().setWriter(new PrintWriter(System.out));
        	engine.getContext().setErrorWriter(new PrintWriter(System.err));
        	
        	engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        	engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(globals);
        	
        	String script = String.format("with(new JavaImporter(java.util, java.lang)) { %s; }", command);
			Object value = (bindings == null) ? engine.eval(script) : engine.eval(script, bindings);
			
			globals.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));
			
			return value;
        } catch(Throwable e) {
        	throw new RuntimeException("Error evaluating command", e);
		}
	}
	
	private static String getScriptExceptionCause(Throwable e) {
		while(e instanceof ScriptException) {
    		e = e.getCause();
    	}
    	return Throwables.getStackTraceAsString(e);
	}
}