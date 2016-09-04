package view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class Console {
	private final Composite parent;
	
	private final StyledText text;

	public Console(Composite parent) {
		this.parent = parent;
		
		this.text = new StyledText(parent, SWT.MULTI | SWT.V_SCROLL);
		
		text.setFont(FontList.consolas10);
		
		text.setEditable(false);
	}
	
	public void addOutput(String output) {
		text.getDisplay().asyncExec(() -> {
			text.append(output + "\n");
		});
	}
	
	public void addError(String error) {
		text.getDisplay().asyncExec(() -> {
			int start = text.getCharCount();
			
			text.append(error + "\n");
			
			StyleRange styleRange = new StyleRange();
			styleRange.start = start;
			styleRange.length = error.length() + 1;
			styleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
			
			text.replaceStyleRanges(start, error.length() + 1, new StyleRange[] { styleRange });
		});
	}

	public Control getControl() {
		return text;
	}
}