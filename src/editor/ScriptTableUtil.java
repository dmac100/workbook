package editor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import javax.script.ScriptException;

import script.Script;

public class ScriptTableUtil {
	private final Script script;

	public ScriptTableUtil(Script script) {
		this.script = script;
	}
	
	private String toCellValue(Object value) {
		return String.valueOf(value);
	}
	
	private String toKeyValue(Object value) {
		return String.valueOf(value);
	}

	/**
	 * Returns a table containing the properties of a single object or list of objects.
	 */
	public Map<String, List<String>> getTable(Object object) throws ScriptException {
		List<Map<String, String>> rows = new ArrayList<>();
		
		if(script.isIterable(object)) {
			script.iterateObject(object, value -> {
				rows.add(getTableRow(value));
			});
		} else if(object instanceof Iterable) {
			Iterable<?> iterable = (Iterable<?>) object;
			iterable.forEach(value -> rows.add(getTableRow(value)));
		} else {
			rows.add(getTableRow(object));
		}
		
		return combineKeys(rows);
	}
	
	/**
	 * Returns a single row of a table containing the properties of an object.
	 */
	private Map<String, String> getTableRow(Object object) {
		Map<String, String> row = new TreeMap<>();
		
		if(script.isScriptObject(object)) {
			Map<?, ?> map = script.getPropertyMap(object);
			map.forEach((k, v) -> {
				row.put(toKeyValue(k), toCellValue(v));
			});
		} else if(object instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) object;
			map.forEach((k, v) -> {
				row.put(toKeyValue(k), toCellValue(v));
			});
		} else if(object != null) {
			iterateJavaObjectProperties(object, (k, v) -> {
				row.put(toKeyValue(k), toCellValue(v));
			});
		}
		
		return row;
	}
	
	/**
	 * Iterate over the properties in a Java object (with get and is methods).
	 */
	private static void iterateJavaObjectProperties(Object object, BiConsumer<String, Object> consumer) {
		for(Method method:object.getClass().getMethods()) {
			if(method.getName().matches("(is|get).*") && method.getParameterCount() == 0) {
				try {
					String name = method.getName().replaceAll("^(is|get)", "");
					if(name.length() > 0) {
						name = name.substring(0, 1).toLowerCase() + name.substring(1);
					}
					method.setAccessible(true);
					Object result = method.invoke(object);
					consumer.accept(name, result);
				} catch(ReflectiveOperationException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Converts a List<Map<String, String>> to a Map<String, List<String>> by combining all keys together.
	 */
	private Map<String, List<String>> combineKeys(List<Map<String, String>> rows) {
		Map<String, List<String>> combinedRows = new TreeMap<>();
		
		if(rows != null) {
			for(Map<String, String> row:rows) {
				for(String key:getAllKeys(rows)) {
					if(!combinedRows.containsKey(key)) {
						combinedRows.put(key, new ArrayList<>());
					}
					combinedRows.get(key).add(toCellValue(row.get(key)));
				}
			}
		}
		
		return combinedRows;
	}
	
	/**
	 * Returns a set of all keys in maps.
	 */
	private static <K, V> Set<K> getAllKeys(Iterable<Map<K, V>> maps) {
		Set<K> keys = new HashSet<>();
		
		maps.forEach(map -> {
			if(map != null) {
				keys.addAll(map.keySet());
			}
		});
		
		return keys;
	}
}
