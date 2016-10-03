package workbook.editor.ui;

import java.util.function.Function;

import org.jdom2.Element;

import workbook.editor.reference.Reference;

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
	
	public void serialize(Element element) {
		Element expression = new Element("Expression");
		expression.setText(this.expression);
		element.addContent(expression);
	}

	public void deserialize(Element element) {
		this.expression = null;
		this.reference = null;
		
		setExpression(element.getChildText("Expression"));
		readValue();
	}
}