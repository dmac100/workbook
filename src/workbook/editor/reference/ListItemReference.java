package workbook.editor.reference;

import java.util.List;
import java.util.function.Supplier;

import workbook.script.Engine;
import workbook.script.ScriptController;

/**
 * A reference to a single value within a Map.
 */
public class ListItemReference extends AbstractScriptReference {
	private final List<Object> list;
	private final int index;

	public <T> ListItemReference(ScriptController scriptController, List<T> list, int index) {
		super(scriptController);
		
		this.list = (List<Object>) list;
		this.index = index;
	}
	
	@Override
	protected void setSync(Engine script, Object value) throws Exception {
		if(!(value instanceof String)) {
			list.set(index, value);
			return;
		}
		
		try {
			Class<?> existingType = list.get(index).getClass();
			
			// Set property based on type of existing value.
			Object convertedValue = convertFromString((String) value, existingType);
			list.set(index, convertedValue);
			return;
		} catch(Exception e) {
		}
		
		// Set property after conversion to any type.
		Object convertedValue = convertFromString((String) value);
		list.set(index, convertedValue);
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
		return list.get(index);
	}
}