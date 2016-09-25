package script;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.script.ScriptException;

public interface Engine {
	public boolean isIterable(Object value) throws ScriptException;
	public void iterateObject(Object array, Consumer<Object> consumer);
	public void setVariable(String name, Object value);
	public Object getVariable(String name);
	public Map<String, Object> getVariableMap();
	public boolean isScriptObject(Object object);
	public Map<Object, Object> getPropertyMap(Object object);
	public Object eval(String command);
	public Object eval(String command, Consumer<String> outputCallback, Consumer<String> errorCallback);
	public List<NameAndProperties> evalWithCallbackFunctions(String command, List<String> callbackFunctionNames, Consumer<String> outputCallback, Consumer<String> errorCallback);
}
