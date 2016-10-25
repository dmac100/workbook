package workbook.editor.ui;

import java.util.function.Function;

import org.eclipse.swt.widgets.Control;
import org.jdom2.Element;

import com.google.common.eventbus.Subscribe;

import workbook.editor.reference.Reference;
import workbook.event.MajorRefreshEvent;
import workbook.event.MinorRefreshEvent;

/**
 * An editor view that can be used to view and modify the contents of a reference.
 */
public abstract class Editor {
	private Function<String, Reference> referenceFunction;
	private String expression;
	protected Reference reference;
	
	/**
	 * Reads the reference value, updating this view.
	 */
	public abstract void readValue();
	
	/**
	 * Returns the control that represents this view.
	 */
	public abstract Control getControl();
	
	@Subscribe
	public void onMinorRefresh(MinorRefreshEvent event) {
		if(event.getSource() != this) {
			readValue();
		}
	}
	
	@Subscribe
	public void onMajorRefresh(MajorRefreshEvent event) {
		readValue();
	}
	
	/**
	 * Sets the expression that is used to read and write values for this view.
	 */
	public void setExpression(String expression) {
		this.expression = expression;
		refreshReference();
	}
	
	/**
	 * Sets a function that is used to get a reference from an expression.
	 */
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