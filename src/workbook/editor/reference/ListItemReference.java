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
		// Don't do type conversion unless value is a String.
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
	
	@Override
	protected Object getSync(Engine script) throws Exception {
		return list.get(index);
	}
}