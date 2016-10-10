package workbook.editor.ui;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.google.common.eventbus.EventBus;

import workbook.view.TabbedView;
import workbook.view.text.EditorText;

public class StringTabbedEditor extends Editor implements TabbedView {
	private final Composite parent;
	private final EditorText editorText;
	
	public StringTabbedEditor(Composite parent, EventBus eventBus) {
		this.parent = parent;
		
		this.editorText = new EditorText(parent);
		
		editorText.getStyledText().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				writeValue();
			}
		});
	}
	
	public void readValue() {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				if(value instanceof String) {
					Display.getDefault().asyncExec(() -> {
						if(!editorText.getControl().isDisposed()) {
							editorText.setText((String)value);
						}
					});
				}
			});
		}
	}
	
	public void writeValue() {
		if(reference != null) {
			reference.set(editorText.getText());
		}
	}

	public Control getControl() {
		return editorText.getControl();
	}
}