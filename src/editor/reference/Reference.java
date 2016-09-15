package editor.reference;

import script.ScriptFuture;

public interface Reference {
	public ScriptFuture<Object> set(Object value);
	public ScriptFuture<Object> get();
}
