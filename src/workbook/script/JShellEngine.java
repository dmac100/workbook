package workbook.script;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.codehaus.groovy.runtime.StackTraceUtils;

import com.github.dmac100.jshellscriptengine.JShellScriptEngine;
import com.google.common.base.Throwables;

import syntaxhighlighter.brush.Brush;
import syntaxhighlighter.brush.BrushJava;

/**
 * An engine using the JShell scripting language.
 */
public class JShellEngine implements Engine {
	private final ScriptEngine engine;
	private Map<String, Object> globals = new HashMap<>();
	
	public JShellEngine() {
		engine = new JShellScriptEngine();
	}
	
	public Brush getBrush() {
		return new BrushJava();
	}
	
	public void setGlobals(Map<String, Object> globals) {
		this.globals = globals;
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
	
	public Object getVariable(String name) {
		return globals.get(name);
	}
	
	public void setVariable(String name, Object value) {
		engine.put(name, value);
		globals.put(name, value);
	}
	
	public boolean isScriptObject(Object object) {
		return false;
	}

	public Map<Object, Object> getPropertyMap(Object object) {
		if(object instanceof Map) {
			return (Map<Object, Object>) object;
		}
		
		return new HashMap<>();
	}
	
	/**
	 * Defines a global function that call the given callback function when called.
	 */
	public void defineFunction(String name, Function<Object, Object> callback) {
		globals.put("_"+name+"Callback", callback);
		eval(String.format("public Object %s(Object param) { return ((java.util.function.Function<Object, Object>) _%sCallback).apply(param); }", name, name));
	}
	
	/**
	 * Evaluates a method given its name and list of parameters, and returns the result.
	 */
	public Object evalMethodCall(String name, List<Object> params) {
		Bindings bindings = engine.createBindings();
		bindings.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));
		for(int x = 0; x < params.size(); x++) {
			bindings.put("_argument" + x, params.get(x));
		}
		
		String argumentsString = "";
		for(int x = 0; x < params.size(); x++) {
			argumentsString += "_argument" + x;
			if(x < params.size() - 1) {
				argumentsString += ", ";
			}
		}
		
		String command = name + "(" + argumentsString + ")";
		
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
		
		bindings.put("callback", new BiConsumer<String, Map<String, Object>>() {
			public void accept(String name, Map<String, Object> properties) {
				Map<String, String> map = new HashMap<>();
				
				for(Object key:new HashSet<>(properties.keySet())) {
					map.put(String.valueOf(key), String.valueOf(properties.get(key)));
				}
				
				NameAndProperties nameAndProperties = new NameAndProperties(name, map);
				callbackValues.add(nameAndProperties);
			}
		});
		
		for(String name:callbackFunctionNames) {
			StringBuilder prefix = new StringBuilder();
			prefix.append(String.format("public void %s(java.util.Map<String, Object> values) {"
				+ "((java.util.function.BiConsumer<String, Map<String, Object>>) callback).accept(\"%s\", values);"
				+ "}", name, name));
			prefix.append("\n");
			eval(prefix.toString(), bindings);
		}
		
		eval(command, bindings);
		
		return callbackValues;
	}
	
	private Object eval(String command, Bindings bindings) {
        try {
        	engine.getContext().setWriter(new PrintWriter(System.out));
        	engine.getContext().setErrorWriter(new PrintWriter(System.err));
        	
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        	engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(globals);
        	
			Object value = (bindings == null) ? engine.eval(command) : engine.eval(command, bindings);
			
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
		StackTraceUtils.sanitize(e);
    	return Throwables.getStackTraceAsString(e);
	}
}
