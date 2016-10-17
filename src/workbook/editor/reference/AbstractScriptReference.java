package workbook.editor.reference;

import workbook.script.Engine;
import workbook.script.ScriptController;
import workbook.script.ScriptFuture;

/**
 * A reference that has access to the script controller.
 */
public abstract class AbstractScriptReference implements Reference {
	protected final ScriptController scriptController;

	/**
	 * Sets the value of the reference synchronously.
	 */
	protected abstract void setSync(Engine script, Object value) throws Exception;
	
	/**
	 * Returns the value of the reference synchronously.
	 */
	protected abstract Object getSync(Engine script) throws Exception;
	
	public AbstractScriptReference(ScriptController scriptController) {
		this.scriptController = scriptController;
	}
	
	@Override
	public ScriptFuture<Void> set(Object value) {
		ScriptFuture<Void> future = new ScriptFuture<>(scriptController);
		scriptController.getScript(script -> {
			try {
				setSync(script, value);
				future.complete(null);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	@Override
	public ScriptFuture<Object> get() {
		ScriptFuture<Object> future = new ScriptFuture<>(scriptController);
		scriptController.getScript(script -> {
			try {
				Object value = getSync(script);
				future.complete(value);
			} catch(Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
}
