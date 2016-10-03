package view;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jdom2.Element;

public class ScriptTabbedView implements TabbedView {
	private final StyledText text;
	
	private Consumer<String> executeCallback;

	public ScriptTabbedView(Composite parent) {
		this.text = new StyledText(parent, SWT.V_SCROLL);
		
		text.addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if(event.keyCode == SWT.CR && event.stateMask == SWT.CONTROL) {
					if(executeCallback != null) {
						executeCallback.accept(text.getText());
						event.doit = false;
					}
				}
			}
		});
		
		text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if((event.stateMask & SWT.CTRL) > 0) {
					if(event.keyCode == 'a') {
						selectAll();
					}
				}
			}
		});
	}
	
	public void selectAll() {
		text.setSelection(0, text.getText().length());
	}

	public Control getControl() {
		return text;
	}

	public void setExecuteCallback(Consumer<String> callback) {
		this.executeCallback = callback;
	}

	public void serialize(Element element) {
		Element content = new Element("Content");
		content.setText(text.getText());
		element.addContent(content);
	}

	public void deserialize(Element element) {
		String content = element.getChildText("Content");
		text.setText(content);
	}
}