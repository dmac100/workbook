package editor;

import java.util.concurrent.CompletableFuture;

import ognl.DefaultClassResolver;
import ognl.DefaultMemberAccess;
import ognl.DefaultTypeConverter;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import script.ScriptController;

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
	public CompletableFuture<Object> set(Object value) {
		CompletableFuture<Object> future = new CompletableFuture<>();
		scriptController.getScript(script -> {
			try {
				Ognl.setValue(expression, context, script.getVariableMap(), value);
				future.complete(null);
			} catch (OgnlException e) {
				e.printStackTrace();
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	@Override
	public CompletableFuture<Object> get() {
		CompletableFuture<Object> future = new CompletableFuture<>();
		scriptController.getScript(script -> {
			try {
				Object value = Ognl.getValue(expression, context, script.getVariableMap());
				future.complete(value);
			} catch (OgnlException e) {
				e.printStackTrace();
				future.completeExceptionally(e);
			}
		});
		return future;
	}
}