package editor;

import java.util.Map;

import script.Script;
import script.ScriptController;

public class MapPropertyReference extends AbstractScriptReference {
	private final Map<String, Object> object;
	private final String property;

	public MapPropertyReference(ScriptController scriptController, Map<String, Object> object, String property) {
		super(scriptController);
		
		this.object = object;
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
