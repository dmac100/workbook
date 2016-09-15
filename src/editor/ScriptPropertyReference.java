package editor;

import script.Script;
import script.ScriptController;

public class ScriptPropertyReference extends AbstractScriptReference {
	private final Object object;
	private final String property;

	public ScriptPropertyReference(ScriptController scriptController, Object object, String property) {
		super(scriptController);
		
		this.object = object;
		this.property = property;
	}
	
	@Override
	protected void setSync(Script script, Object value) throws Exception {
		script.getPropertyMap(object).put(property, value);
	}

	@Override
	protected Object getSync(Script script) throws Exception {
		return script.getPropertyMap(object).get(property);
	}
}
