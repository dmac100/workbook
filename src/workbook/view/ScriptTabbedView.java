package workbook.view;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import workbook.event.ScriptTypeChangeEvent;
import workbook.model.Model;
import workbook.view.text.EditorText;

public class ScriptTabbedView implements TabbedView {
	private final EditorText editorText;
	private final Model model;
	
	private Consumer<String> executeCallback;

	public ScriptTabbedView(Composite parent, EventBus eventBus, Model model) {
		this.editorText = new EditorText(parent);
		this.model = model;
		
		editorText.getStyledText().addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if(event.keyCode == SWT.CR && event.stateMask == SWT.CONTROL) {
					if(executeCallback != null) {
						executeCallback.accept(editorText.getText());
						event.doit = false;
					}
				}
			}
		});
		
		refreshBrush();
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
	@Subscribe
	public void onScriptTypeChange(ScriptTypeChangeEvent event) {
		refreshBrush();
	}
	
	private void refreshBrush() {
		Display.getDefault().asyncExec(() -> editorText.setBrush(model.getBrush()));
	}
	
	public Control getControl() {
		return editorText.getControl();
	}

	public void setExecuteCallback(Consumer<String> callback) {
		this.executeCallback = callback;
	}

	public void serialize(Element element) {
		Element content = new Element("Content");
		content.setText(editorText.getText());
		element.addContent(content);
	}

	public void deserialize(Element element) {
		String content = element.getChildText("Content");
		editorText.setText(content);
	}
}