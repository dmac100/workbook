package workbook.view.result;

import workbook.view.result.wrapper.Wrapper;

/**
 * A wrapper for a result value that will be displayed in an editor of editorType.
 */
public class EditorResult {
	private final Object value;
	private final String editorType;
	private final int height;
	
	public EditorResult(Object value, String editorType, int height) {
		this.value = value;
		this.editorType = editorType;
		this.height = height;
	}
	
	public EditorResult(Object value, String editorType) {
		this.value = value;
		this.editorType = editorType;
		this.height = 300;
	}

	public Object getValue() {
		// Unwrap value if it's a Wrapper.
		if(value instanceof Wrapper) {
			return ((Wrapper) value).getValue();
		}
		
		return value;
	}

	public int getHeight() {
		return height;
	}

	public String getEditorType() {
		return editorType;
	}
}