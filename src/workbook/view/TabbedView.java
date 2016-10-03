package workbook.view;

import org.eclipse.swt.widgets.Control;
import org.jdom2.Element;

public interface TabbedView {
	public Control getControl();
	public void serialize(Element element);
	public void deserialize(Element element);
}
