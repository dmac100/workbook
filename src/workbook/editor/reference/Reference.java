package workbook.editor.reference;

import workbook.script.ScriptFuture;

public interface Reference {
	public ScriptFuture<Void> set(Object value);
	public ScriptFuture<Object> get();
}
