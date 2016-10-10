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

import workbook.event.ScriptTypeChange;
import workbook.view.text.EditorText;

public class ScriptTabbedView implements TabbedView {
	private final EditorText editorText;
	
	private Consumer<String> executeCallback;

	public ScriptTabbedView(Composite parent, EventBus eventBus) {
		this.editorText = new EditorText(parent);
		
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
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
	@Subscribe
	public void onScriptTypeChange(ScriptTypeChange event) {
		Display.getDefault().asyncExec(() -> editorText.setBrush(event.getBrush()));
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