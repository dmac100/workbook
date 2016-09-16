package editor.ui;

import editor.reference.Reference;

public interface Editor {
	public void setReference(Reference reference);
	public String getExpression();
	public void readValue();
}
