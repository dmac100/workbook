package workbook.editor.reference;

import workbook.script.Engine;
import workbook.script.ScriptController;

/**
 * A reference to a constant value.
 */
public class ConstantReference extends AbstractScriptReference {
	private final Object value;

	public <K, V> ConstantReference(ScriptController scriptController, Object value) {
		super(scriptController);
		
		this.value = value;
	}
	
	@Override
	protected Object getSync(Engine script) throws Exception {
		return value;
	}

	@Override
	protected void setSync(Engine script, Object value) throws Exception {
	}
}