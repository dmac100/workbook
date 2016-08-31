package script;

import java.io.PrintStream;
import java.util.function.Consumer;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class Script {
	private final ScriptEngine engine;
	
	public Script() {
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		engine = factory.getScriptEngine(new String[] { "--class-cache-size = 0" });
		if(engine == null) {
			throw new RuntimeException("Can't create JavaScript engine");
		}
		
		eval("function print() { System.out.println([].slice.call(arguments).join(', ')) }");
	}
	
	public void addVariable(String name, Object value) {
		engine.put(name, value);
	}
	
	public String eval(String command) {
		Consumer<String> nullCallback = x -> {};
		
		try {
			return eval(command, nullCallback, nullCallback);
		} catch(ScriptException e) {
			throw new RuntimeException("Error evaluating command", e);
		}
	}
	
	public String eval(String command, Consumer<String> outputCallback, Consumer<String> errorCallback) throws ScriptException {
        PrintStream out = System.out;
        PrintStream err = System.err;
        try {
        	LineReader outputReader = new LineReader(outputCallback);
        	LineReader errorReader = new LineReader(errorCallback);
        	
	        System.setOut(new PrintStream(outputReader.getOutputStream()));
	        System.setErr(new PrintStream(errorReader.getOutputStream()));
	        
			Object output = engine.eval("with(new JavaImporter(java.util, java.lang)) { " + command + "}");
			String value = String.valueOf(output);

			System.out.close();
			System.err.close();
			
			outputReader.waitUntilDone();
			errorReader.waitUntilDone();
			
			return value;
        } finally {
        	System.setOut(out);
        	System.setErr(err);
        }
	}
}