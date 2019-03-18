package workbook.script;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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
	public void defineFunction(String name, Function<Object, Object> callback);
	public Object eval(String command);
	public List<NameAndProperties> evalWithCallbackFunctions(String command, List<String> callbackFunctionNames);
	public Object evalMethodCall(String methodName, List<Object> params);
}