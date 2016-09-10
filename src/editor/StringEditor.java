package editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class StringEditor implements Editor {
	private final Composite parent;
	private final String expression;
	
	private final StyledText text;
	
	private Reference reference;

	public StringEditor(Composite parent, String expression) {
		this.parent = parent;
		this.expression = expression;
		
		text = new StyledText(parent, SWT.V_SCROLL);
		
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				writeValue();
			}
		});
	}
	
	public String getExpression() {
		return expression;
	}
	
	public void setReference(Reference reference) {
		this.reference = reference;
		readValue();
	}
	
	public void readValue() {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				if(value instanceof String) {
					text.getDisplay().asyncExec(() -> {
						text.setText((String)value);
					});
				}
			});
		}
	}
	
	public void writeValue() {
		if(reference != null) {
			reference.set(text.getText());
		}
	}

	public Control getControl() {
		return text;
	}
}