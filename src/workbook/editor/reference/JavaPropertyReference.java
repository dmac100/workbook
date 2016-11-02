package workbook.editor.reference;

import java.lang.reflect.Method;

import workbook.script.Engine;
import workbook.script.ScriptController;

/**
 * A reference to a Java property through a pair of get and set methods.
 */
public class JavaPropertyReference extends AbstractScriptReference {
	private final Method getMethod;
	private final Method setMethod;
	private final Object object;

	public JavaPropertyReference(ScriptController scriptController, Object object, Method getMethod, Method setMethod) {
		super(scriptController);
		
		this.object = object;
		this.getMethod = getMethod;
		this.setMethod = setMethod;
	}
	
	@Override
	protected void setSync(Engine script, Object value) throws Exception {
		// Don't do type conversion unless value is a String.
		if(setMethod != null) {
			Object convertedValue = value;
			if(value instanceof String) {
				convertedValue = convertFromString((String) value, setMethod.getParameterTypes()[0]);
			}
			setMethod.invoke(object, convertedValue);
		}
	}

	@Override
	protected Object getSync(Engine script) throws Exception {
		if(getMethod == null) {
			return null;
		} else {
			return getMethod.invoke(object);
		}
	}
}
