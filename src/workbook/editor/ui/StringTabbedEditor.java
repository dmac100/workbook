package workbook.editor.ui;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.google.common.eventbus.EventBus;

import workbook.event.MinorRefreshEvent;
import workbook.view.TabbedView;
import workbook.view.text.EditorText;

public class StringTabbedEditor extends Editor implements TabbedView {
	private final Composite parent;
	private final EventBus eventBus;
	private final EditorText editorText;
	
	private boolean disableModifyCallback;
	
	public StringTabbedEditor(Composite parent, EventBus eventBus) {
		this.parent = parent;
		this.eventBus = eventBus;
		
		this.editorText = new EditorText(parent);
		
		editorText.getStyledText().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				if(!disableModifyCallback) {
					writeValue();
				}
			}
		});
	}
	
	public void readValue() {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				if(value instanceof String) {
					Display.getDefault().asyncExec(() -> {
						if(!editorText.getControl().isDisposed()) {
							disableModifyCallback = true;
							editorText.setText((String)value);
							disableModifyCallback = false;
						}
					});
				}
			});
		}
	}
	
	public void writeValue() {
		if(reference != null) {
			reference.set(editorText.getText()).thenRun(() -> {
				eventBus.post(new MinorRefreshEvent());
			});
		}
	}

	public Control getControl() {
		return editorText.getControl();
	}
}