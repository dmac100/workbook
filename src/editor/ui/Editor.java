package editor.ui;

import java.util.function.Function;

import editor.reference.Reference;

public abstract class Editor {
	private Function<String, Reference> referenceFunction;
	private String expression;
	protected Reference reference;
	
	public abstract void readValue();
	
	public void setExpression(String expression) {
		this.expression = expression;
		refreshReference();
	}
	
	public void setReferenceFunction(Function<String, Reference> referenceFunction) {
		this.referenceFunction = referenceFunction;
		refreshReference();
	}
	
	private void refreshReference() {
		if(referenceFunction != null && expression != null) {
			reference = referenceFunction.apply(expression);
			readValue();
		}
	}
}
