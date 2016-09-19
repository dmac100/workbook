package view;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ScriptEditor implements View {
	private final StyledText text;
	
	private Consumer<String> executeCallback;

	public ScriptEditor(Composite parent) {
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
	}

	public Control getControl() {
		return text;
	}

	public void setExecuteCallback(Consumer<String> callback) {
		this.executeCallback = callback;
	}
}
