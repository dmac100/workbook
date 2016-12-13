package workbook.editor.ui;

import org.eclipse.swt.widgets.Control;
import org.jdom2.Element;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import workbook.editor.reference.OgnlReference;
import workbook.editor.reference.Reference;
import workbook.event.MajorRefreshEvent;
import workbook.event.MinorRefreshEvent;
import workbook.script.ScriptController;

/**
 * An editor view that can be used to view and modify the contents of a reference.
 */
public abstract class Editor {
	private final ScriptController scriptController;
	private final EventBus eventBus;
	
	private String expression;
	protected Reference reference;
	
	protected Editor(EventBus eventBus, ScriptController scriptController) {
		this.eventBus = eventBus;
		this.scriptController = scriptController;
	}
	
	/**
	 * Register event listeners on the EventBus and removes them when disposed.
	 */
	protected void registerEvents() {
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
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
		this.reference = new OgnlReference(scriptController, expression);
		readValue();
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