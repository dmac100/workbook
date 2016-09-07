package editor;

public interface Editor {
	public void setReference(Reference reference);
	public String getExpression();
	public void readValue();
	public void writeValue();
}
