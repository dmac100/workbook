package workbook.editor.reference;

import workbook.script.ScriptFuture;

/**
 * A reference that can be used to asynchronously get or set a value.
 */
public interface Reference {
	/**
	 * Sets the value of the reference, returning a future that will complete when this is done.
	 */
	public ScriptFuture<Void> set(Object value);
	
	/**
	 * Gets the value of the reference, returning a future that will complete with this value.
	 */
	public ScriptFuture<Object> get();
}
