package editor;

import ognl.DefaultClassResolver;
import ognl.DefaultMemberAccess;
import ognl.DefaultTypeConverter;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import script.Script;
import script.ScriptController;

public class OgnlReference extends AbstractScriptReference {
	private final OgnlContext context;
	
	private Object expression = null;

	public OgnlReference(ScriptController scriptController, String expression) {
		super(scriptController);
		
		this.context = new OgnlContext(new DefaultClassResolver(), new DefaultTypeConverter(), new DefaultMemberAccess(true));
		
		try {
			this.expression = Ognl.parseExpression(expression);
		} catch (OgnlException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setSync(Script script, Object value) throws Exception {
		Ognl.setValue(expression, context, script.getVariableMap(), value);
	}

	@Override
	protected Object getSync(Script script) throws Exception {
		return Ognl.getValue(expression, context, script.getVariableMap());
	}
}