package workbook.view.result;

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
		return value;
	}

	public int getHeight() {
		return height;
	}

	public String getEditorType() {
		return editorType;
	}
}