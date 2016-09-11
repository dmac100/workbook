package editor;

import ognl.DefaultClassResolver;
import ognl.DefaultMemberAccess;
import ognl.DefaultTypeConverter;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import script.ScriptController;
import script.ScriptFuture;

public class OgnlReference implements Reference {
	private final OgnlContext context;
	
	private Object expression = null;
	private final ScriptController scriptController;

	public OgnlReference(ScriptController scriptController, String expression) {
		this.context = new OgnlContext(new DefaultClassResolver(), new DefaultTypeConverter(), new DefaultMemberAccess(true));
		this.scriptController = scriptController;
		try {
			this.expression = Ognl.parseExpression(expression);
		} catch (OgnlException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ScriptFuture<Object> set(Object value) {
		ScriptFuture<Object> future = new ScriptFuture<>();
		scriptController.getScript(script -> {
			try {
				Ognl.setValue(expression, context, script.getVariableMap(), value);
				future.complete(null);
			} catch (OgnlException e) {
				throw new RuntimeException("Error setting ognl value", e);
			}
		});
		return future;
	}

	@Override
	public ScriptFuture<Object> get() {
		ScriptFuture<Object> future = new ScriptFuture<>();
		scriptController.getScript(script -> {
			try {
				Object value = Ognl.getValue(expression, context, script.getVariableMap());
				future.complete(value);
			} catch (OgnlException e) {
				throw new RuntimeException("Error getting ognl value", e);
			}
		});
		return future;
	}
}