package editor;

import script.Script;

public class ScriptBindingReference implements Reference {
	private final Script script;
	private final String name;
	
	public ScriptBindingReference(Script script, String name) {
		this.script = script;
		this.name = name;
	}
	
	@Override
	public void set(Object value) {
		script.addVariable(name, value);
	}

	@Override
	public Object get() {
		return script.getVariable(name);
	}
}
