package editor.reference;

import script.Engine;
import script.ScriptController;
import script.ScriptFuture;

public abstract class AbstractScriptReference implements Reference {
	private final ScriptController scriptController;

	protected abstract void setSync(Engine script, Object value) throws Exception;
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
