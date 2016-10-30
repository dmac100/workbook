package workbook.script;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import syntaxhighlighter.brush.Brush;

/**
 * An engine that allows the running of commands, and querying of their results.
 */
public interface Engine {
	public Brush getBrush();
	public void setGlobals(Map<String, Object> globals);
	public boolean isIterable(Object value);
	public void iterateObject(Object array, Consumer<Object> consumer);
	public void setVariable(String name, Object value);
	public Object getVariable(String name);
	public boolean isScriptObject(Object object);
	public Map<Object, Object> getPropertyMap(Object object);
	public Object eval(String command, Consumer<String> outputCallback, Consumer<String> errorCallback);
	public List<NameAndProperties> evalWithCallbackFunctions(String command, List<String> callbackFunctionNames, Consumer<String> outputCallback, Consumer<String> errorCallback);
	public Object evalMethodCall(String methodName, List<Object> params, Consumer<String> outputCallback, Consumer<String> errorCallback);
	
	public default Object eval(String command) {
		return eval(command, x -> {}, x -> {});
	}
	
	public default Object evalMethodCall(String methodName, List<Object> params) {
		return evalMethodCall(methodName, params, x -> {}, x -> {});
	}
}