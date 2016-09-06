package editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class StringEditor implements Editor {
	private final Composite parent;
	private final String name;
	
	private final StyledText text;
	
	private Reference reference;

	public StringEditor(Composite parent, String name) {
		this.parent = parent;
		this.name = name;
		
		text = new StyledText(parent, SWT.NONE);
		
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				writeValue();
			}
		});
	}
	
	public String getValue() {
		return text.getText();
	}
	
	public void setReference(Reference reference) {
		this.reference = reference;
	}
	
	public void readValue() {
		if(reference != null) {
			Object value = reference.get();
			if(value instanceof String) {
				text.setText((String)value);
			}
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

	public String getName() {
		return name;
	}
}