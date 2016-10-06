package workbook.editor.reference;

import workbook.script.Engine;
import workbook.script.ScriptController;

public class GlobalVariableReference extends AbstractScriptReference {
	private final String expression;

	public GlobalVariableReference(ScriptController scriptController, String expression) {
		super(scriptController);
		
		this.expression = expression;
	}

	@Override
	protected void setSync(Engine script, Object value) throws Exception {
		scriptController.getScriptSync().setVariable(expression, value);
	}

	@Override
	protected Object getSync(Engine script) throws Exception {
		return scriptController.getScriptSync().getVariable(expression);
	}
}