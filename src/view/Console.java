package view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class Console {
	private final Composite parent;
	
	private final Text text;

	public Console(Composite parent) {
		this.parent = parent;
		
		this.text = new Text(parent, SWT.MULTI | SWT.V_SCROLL);
		
		text.setEditable(false);
	}

	public Control getControl() {
		return text;
	}
}
