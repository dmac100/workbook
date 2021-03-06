package workbook.editor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import workbook.editor.reference.ConstantReference;
import workbook.editor.reference.JavaPropertyReference;
import workbook.editor.reference.ListItemReference;
import workbook.editor.reference.MapPropertyReference;
import workbook.editor.reference.Reference;
import workbook.script.Engine;
import workbook.script.ScriptController;

/**
 * Converts an object into a table based on the properties within it.
 */
public class ScriptTableUtil {
	private final ScriptController scriptController;

	public ScriptTableUtil(ScriptController scriptController) {
		this.scriptController = scriptController;
	}
	
	private String toKeyValue(Object value) {
		return String.valueOf(value);
	}
	
	/**
	 * Returns whether an object is iterable, either in Java, or as a script object.
	 */
	public boolean isIterable(Object object) {
		Engine script = scriptController.getScriptSync();
		
		return (object instanceof Iterable || script.isIterable(object));
	}

	/**
	 * Returns a table containing the properties of a single object or list of objects.
	 */
	public Map<String, List<Reference>> getTable(Object object) {
		List<Map<String, Reference>> rows = new ArrayList<>();
		List<Object> objects = new ArrayList<>();
		
		Engine script = scriptController.getScriptSync();
		
		// Add row for object of each element if it's iterable.
		if(script.isIterable(object)) {
			script.iterateObject(object, value -> {
				rows.add(getTableRow(value));
				objects.add(value);
			});
		} else if(object instanceof Iterable) {
			Iterable<?> iterable = (Iterable<?>) object;
			iterable.forEach(value -> {
				rows.add(getTableRow(value));
				objects.add(value);
			});
		} else {
			rows.add(getTableRow(object));
			objects.add(object);
		}
		
		// Add missing properties to each object that exist in other objects.
		for(String key:getAllKeys(rows)) {
			for(int i = 0; i < rows.size(); i++) {
				Map<String, Reference> row = rows.get(i);
				if(!row.containsKey(key)) {
					row.put(key, getNewPropertyReference(objects.get(i), key));
				}
			}
		}
		
		return combineKeys(rows);
	}
	
	/**
	 * Returns a single row of a table containing the properties of an object.
	 */
	public Map<String, Reference> getTableRow(Object object) {
		Map<String, Reference> row = new LinkedHashMap<>();
		
		Engine script = scriptController.getScriptSync();
		
		if(script.isScriptObject(object)) {
			Map<Object, Object> map = script.getPropertyMap(object);
			map.forEach((k, v) -> {
				row.put(toKeyValue(k), new MapPropertyReference(scriptController, map, k));
			});
		} else if(object instanceof Map) {
			Map<Object, Object> map = (Map<Object, Object>) object;
			map.forEach((k, v) -> {
				row.put(toKeyValue(k), new MapPropertyReference(scriptController, map, k));
			});
		} else if(object instanceof List) {
			List<Object> list = (List<Object>) object;
			for(int i = 0; i < list.size(); i++) {
				row.put(String.valueOf(i), new ListItemReference(scriptController, list, i));
			}
		} else if(object instanceof Iterable) {
			Iterable iterable = (Iterable) object;
			Iterator iterator = iterable.iterator();
			int i = 0;
			while(iterator.hasNext()) {
				row.put(String.valueOf(i++), new ConstantReference(scriptController, iterator.next()));
			}
		} else if(object != null) {
			iterateJavaObjectProperties(object, (k, v) -> {
				row.put(toKeyValue(k), v);
			});
		}
		
		return row;
	}
	
	/**
	 * Returns a reference to modify an non-existing property of an object.
	 */
	private Reference getNewPropertyReference(Object object, String key) {
		Engine script = scriptController.getScriptSync();
		
		if(script.isScriptObject(object)) {
			Map<Object, Object> map = script.getPropertyMap(object);
			return new MapPropertyReference(scriptController, map, key);
		} else if(object instanceof Map) {
			Map<Object, Object> map = (Map<Object, Object>) object;
			return new MapPropertyReference(scriptController, map, key);
		} else if(object != null) {
			return null;
		}
		
		return null;
	}
	
	/**
	 * Iterate over the properties in a Java object (with get, set and is methods).
	 */
	private void iterateJavaObjectProperties(Object object, BiConsumer<String, Reference> consumer) {
		Method[] methods = object.getClass().getMethods();
		Arrays.sort(methods, Comparator.comparing(Method::getName));
		
		for(Method getMethod:methods) {
			if(getMethod.getName().equals("getClass")) continue;
			
			if(getMethod.getName().matches("(is|get).*") && getMethod.getParameterCount() == 0) {
				String name = getMethod.getName().replaceAll("^(is|get)", "");
				Method setMethod = getSetMethod(object.getClass(), name);
				if(name.length() > 0) {
					name = name.substring(0, 1).toLowerCase() + name.substring(1);
				}
				getMethod.setAccessible(true);
				consumer.accept(name, new JavaPropertyReference(scriptController, object, getMethod, setMethod));
			}
		}
	}

	/**
	 * Returns the set method corresponding to a property on and class.
	 */
	private static Method getSetMethod(Class<?> clazz, String name) {
		for(Method setMethod:clazz.getMethods()) {
			if(setMethod.getName().equals("set" + name) && setMethod.getParameterCount() == 1) {
				setMethod.setAccessible(true);
				return setMethod;
			}
		}
		return null;
	}

	/**
	 * Converts a List<Map<A, B>> to a Map<A, List<B>> by combining all keys together.
	 */
	private <A, B> Map<A, List<B>> combineKeys(List<Map<A, B>> rows) {
		Map<A, List<B>> combinedRows = new LinkedHashMap<>();
		
		if(rows != null) {
			for(Map<A, B> row:rows) {
				for(A key:getAllKeys(rows)) {
					if(!combinedRows.containsKey(key)) {
						combinedRows.put(key, new ArrayList<>());
					}
					combinedRows.get(key).add(row.get(key));
				}
			}
		}
		
		return combinedRows;
	}
	
	/**
	 * Returns a set of all keys in maps.
	 */
	private static <K, V> Set<K> getAllKeys(Iterable<Map<K, V>> maps) {
		Set<K> keys = new LinkedHashSet<>();
		
		maps.forEach(map -> {
			if(map != null) {
				keys.addAll(map.keySet());
			}
		});
		
		return keys;
	}
}
