package workbook.view;

import org.eclipse.swt.widgets.Control;
import org.jdom2.Element;

/**
 * A view that can appear within the tabbed layout.
 */
public interface TabbedView {
	/**
	 * Returns the control that represents this view.
	 */
	public Control getControl();
	
	/**
	 * Serializes the contents of this view to an XML element.
	 */
	public void serialize(Element element);
	
	/**
	 * Deserializes the contents of this view from an XML element.
	 */
	public void deserialize(Element element);
}
