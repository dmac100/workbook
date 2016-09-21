package view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;

public class Console implements TabbedView {
	private final Composite parent;
	private final StyledText text;
	
	public Console(Composite parent) {
		this.parent = parent;
		
		text = new StyledText(parent, SWT.WRAP | SWT.V_SCROLL);
		text.setFont(FontList.consolas10);
		text.setEditable(false);
	}
	
	public void addOutput(String output) {
		text.getDisplay().asyncExec(() -> {
			text.append(output);
			text.setTopIndex(text.getLineCount() - 1);
		});
	}
	
	public void addError(String error) {
		text.getDisplay().asyncExec(() -> {
			int start = text.getCharCount();
			
			text.append(error);
			
			StyleRange styleRange = new StyleRange();
			styleRange.start = start;
			styleRange.length = error.length();
			styleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
			
			text.replaceStyleRanges(start, error.length(), new StyleRange[] { styleRange });
		});
	}

	public Control getControl() {
		return text;
	}

	public void serialize(Element element) {
	}

	public void deserialize(Element element) {
	}
}