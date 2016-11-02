package workbook.editor.reference;

import java.util.function.Supplier;

import workbook.script.Engine;
import workbook.script.ScriptController;
import workbook.script.ScriptFuture;

/**
 * A reference that has access to the script controller.
 */
public abstract class AbstractScriptReference implements Reference {
	protected final ScriptController scriptController;

	/**
	 * Sets the value of the reference synchronously.
	 */
	protected abstract void setSync(Engine script, Object value) throws Exception;
	
	/**
	 * Returns the value of the reference synchronously.
	 */
	protected abstract Object getSync(Engine script) throws Exception;
	
	public AbstractScriptReference(ScriptController scriptController) {
		this.scriptController = scriptController;
	}
	
	@Override
	public ScriptFuture<Void> set(Object value) {
		ScriptFuture<Void> future = new ScriptFuture<>(scriptController);
		scriptController.getScript(script -> {
			try {
				setSync(script, value);
				future.complete(null);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	@Override
	public ScriptFuture<Object> get() {
		ScriptFuture<Object> future = new ScriptFuture<>(scriptController);
		scriptController.getScript(script -> {
			try {
				Object value = getSync(script);
				future.complete(value);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	/**
	 * Converts a value from a String type to the given type.
	 */
	protected static Object convertFromString(String value, Class<?> type) {
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
	protected static Object convertFromString(String value) {
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
}
