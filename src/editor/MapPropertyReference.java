package editor;

import java.util.Map;

import script.Script;
import script.ScriptController;

public class MapPropertyReference extends AbstractScriptReference {
	private final Map<Object, Object> object;
	private final Object property;

	public <K, V> MapPropertyReference(ScriptController scriptController, Map<K, V> object, K property) {
		super(scriptController);
		
		this.object = (Map<Object, Object>) object;
		this.property = property;
	}
	
	@Override
	protected void setSync(Script script, Object value) throws Exception {
		object.put(property, value);
	}

	@Override
	protected Object getSync(Script script) throws Exception {
		return object.get(property);
	}
}
