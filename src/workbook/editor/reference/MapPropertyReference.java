package workbook.editor.reference;

import java.util.Map;
import java.util.function.Supplier;

import workbook.script.Engine;
import workbook.script.ScriptController;

public class MapPropertyReference extends AbstractScriptReference {
	private final Map<Object, Object> object;
	private final Object property;

	public <K, V> MapPropertyReference(ScriptController scriptController, Map<K, V> object, K property) {
		super(scriptController);
		
		this.object = (Map<Object, Object>) object;
		this.property = property;
	}
	
	@Override
	protected void setSync(Engine script, Object value) throws Exception {
		if(!(value instanceof String)) {
			object.put(property, value);
			return;
		}
		
		if(object.containsKey(property)) {
			try {
				Class<?> existingType = object.get(property).getClass();
				
				// Set property based on type of existing value.
				Object convertedValue = convertFromString((String) value, existingType);
				object.put(property, convertedValue);
				return;
			} catch(Exception e) {
			}
		}
		
		// Set property after conversion to any type.
		Object convertedValue = convertFromString((String) value);
		object.put(property, convertedValue);
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
	
	/**
	 * Converts a value from a String to any matching type.
	 */
	private static Object convertFromString(String value) {
		if(value == null) return null;
		
		if(value.equalsIgnoreCase("null")) return null;
		if(value.equalsIgnoreCase("true")) return true;
		if(value.equalsIgnoreCase("false")) return false;
		
		return tryUntilSuccess(
			() -> Integer.parseInt(value),
			() -> Long.parseLong(value),
			() -> Double.parseDouble(value),
			() -> Float.parseFloat(value),
			() -> Short.parseShort(value),
			() -> Byte.parseByte(value),
			() -> value
		);
	}

	@SafeVarargs
	private static <T> T tryUntilSuccess(Supplier<T>... suppliers) {
		for(Supplier<T> supplier:suppliers) {
			try {
				return supplier.get();
			} catch(Exception e) {
			}
		}
		return null;
	}

	@Override
	protected Object getSync(Engine script) throws Exception {
		return object.get(property);
	}
}