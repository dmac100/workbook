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

import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyObject;
import org.jruby.embed.jsr223.JRubyEngineFactory;

import com.google.common.base.Throwables;

import syntaxhighlighter.brush.Brush;
import syntaxhighlighter.brush.BrushRuby;

/**
 * An engine using the Ruby scripting language.
 */
public class RubyEngine implements Engine {
	private final ScriptEngine engine;
	private Map<String, Object> globals = new HashMap<>();
	
	public RubyEngine() {
		System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
		
		engine = new JRubyEngineFactory().getScriptEngine();
		if(engine == null) {
			throw new RuntimeException("Can't create JRuby engine");
		}
	}
	
	public Brush getBrush() {
		return new BrushRuby();
	}
	
	public void setGlobals(Map<String, Object> globals) {
		this.globals = globals;
	}
	
	public boolean isIterable(Object value) {
		return (value instanceof RubyArray);
	}

	public void iterateObject(Object array, Consumer<Object> consumer) {
		RubyArray rubyArray = (RubyArray) array;
		for(int i = 0; i < rubyArray.getLength(); i++) {
			consumer.accept(rubyArray.get(i));
		}
	}
	
	public void setVariable(String name, Object value) {
		engine.put(name, value);
		globals.put(name, value);
	}
	
	public Object getVariable(String name) {
		return globals.get(name);
	}
	
	public boolean isScriptObject(Object object) {
		return (object instanceof RubyObject);
	}

	public Map<Object, Object> getPropertyMap(Object object) {
		if(object instanceof RubyHash) {
			return (Map<Object, Object>) object;
		}
		
		return new HashMap<>();
	}
	
	/**
	 * Defines a global function that call the given callback function when called.
	 */
	public void defineFunction(String name, Function<Object, Object> callback) {
		globals.put(name, callback);
		eval("$" + name + " = " + name);
		eval(String.format("def %s(param) $%s.apply(param); end", name, name));
	}
	
	/**
	 * Evaluates a method given its name and list of parameters, and returns the result.
	 */
	public Object evalMethodCall(String name, List<Object> params) {
		Bindings bindings = engine.createBindings();
		bindings.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));
		bindings.put("arguments", params);
		String command = name + "(*arguments)";
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
		
		StringBuilder prefix = new StringBuilder("$callback = callback\n");
		for(String name:callbackFunctionNames) {
			prefix.append(String.format("def %s(values) $callback.accept('%s', java.util.HashMap.new(values)); end", name, name));
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
        	
        	String script = String.format("require 'java'; %s;", command);
			Object value = (bindings == null) ? engine.eval(script) : engine.eval(script, bindings);
			
			globals.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));
			
			engine.getContext().getWriter().flush();
			engine.getContext().getErrorWriter().flush();
			
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