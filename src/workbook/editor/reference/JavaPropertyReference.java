package workbook.editor.reference;

import java.lang.reflect.Method;

import workbook.script.Engine;
import workbook.script.ScriptController;

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
	
	/**
	 * Converts a value from a String type to the given type.
	 */
	private static Object convertFromString(String value, Class<?> type) {
		if(value == null) return null;
		
		if(value.equalsIgnoreCase("null") && !type.isPrimitive()) return null;
		
		if(type == Boolean.TYPE || type == Boolean.class) return Boolean.parseBoolean(value);
		if(type == Byte.TYPE || type == Byte.class) return Byte.parseByte(value);
		if(type == Character.TYPE || type == Character.class) return value.charAt(0);
		if(type == Short.TYPE || type == Short.class) return Short.parseShort(value);
		if(type == Integer.TYPE || type == Integer.class) return Integer.parseInt(value);
		if(type == Long.TYPE || type == Long.class) return Long.parseLong(value);
		if(type == Float.TYPE || type == Float.class) return Float.parseFloat(value);
		if(type == Double.TYPE || type == Double.class) return Double.parseDouble(value);
		
		return value;
	}
}
