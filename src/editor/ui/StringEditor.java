package editor.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import view.TabbedView;

public class StringEditor extends Editor implements TabbedView {
	private final Composite parent;
	private final StyledText text;
	
	public StringEditor(Composite parent) {
		this.parent = parent;
		
		text = new StyledText(parent, SWT.V_SCROLL);
		
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				writeValue();
			}
		});
	}
	
	public void readValue() {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				if(value instanceof String) {
					text.getDisplay().asyncExec(() -> {
						if(!text.isDisposed()) {
							text.setText((String)value);
						}
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