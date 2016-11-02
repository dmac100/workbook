package workbook.editor.reference;

import java.util.Map;

import workbook.script.Engine;
import workbook.script.ScriptController;

/**
 * A reference to a single value within a Map.
 */
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
		// Don't do type conversion unless value is a String.
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
	
	@Override
	protected Object getSync(Engine script) throws Exception {
		return object.get(property);
	}
}