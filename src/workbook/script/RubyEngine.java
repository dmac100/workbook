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
import javax.script.ScriptException;

import org.codehaus.groovy.runtime.StackTraceUtils;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyObject;

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
		
		engine = new ScriptEngineManager().getEngineByName("jruby");
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
		return engine.get(name);
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
	
	public Object eval(String command) {
		Consumer<String> nullCallback = x -> {};
		return eval(command, nullCallback, nullCallback);
	}
	
	/**
	 * Evaluates a command, and returns the result.
	 */
	public Object eval(String command, Consumer<String> outputCallback, Consumer<String> errorCallback) {
		return eval(command, null, outputCallback, errorCallback);
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
		
		StringBuilder prefix = new StringBuilder("$callback = callback\n");
		for(String name:callbackFunctionNames) {
			prefix.append(String.format("def %s(values) $callback.accept('%s', java.util.HashMap.new(values)); end", name, name));
			prefix.append("\n");
		}
		
		eval(prefix.toString(), bindings, outputCallback, errorCallback);
		eval(command, bindings, outputCallback, errorCallback);
		
		return callbackValues;
	}
	
	private Object eval(String command, Bindings bindings, Consumer<String> outputCallback, Consumer<String> errorCallback) {
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
        	
        	String script = String.format("require 'java'; %s;", command);
			Object value = (bindings == null) ? engine.eval(script) : engine.eval(script, bindings);
			
			globals.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));
			
			outputWriter.close();
			errorWriter.close();
			
			outputReader.waitUntilDone();
			errorReader.waitUntilDone();
			
			return value;
        } catch(Exception e) {
        	e.printStackTrace(err);
        	errorCallback.accept(getScriptExceptionCause(e));
        	return null;
        } finally {
        	System.setOut(out);
        	System.setErr(err);
        }
	}
	
	private static String getScriptExceptionCause(Throwable e) {
		while(e instanceof ScriptException) {
    		e = e.getCause();
    	}
    	return Throwables.getStackTraceAsString(e);
	}
}