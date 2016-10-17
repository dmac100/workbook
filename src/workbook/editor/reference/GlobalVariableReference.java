package workbook.editor.reference;

import workbook.script.Engine;
import workbook.script.ScriptController;

/**
 * A reference to a global variable within the script engine.
 */
public class GlobalVariableReference extends AbstractScriptReference {
	private final String name;

	public GlobalVariableReference(ScriptController scriptController, String name) {
		super(scriptController);
		
		this.name = name;
	}

	@Override
	protected void setSync(Engine script, Object value) throws Exception {
		script.setVariable(name, value);
	}

	@Override
	protected Object getSync(Engine script) throws Exception {
		return script.getVariable(name);
	}
}