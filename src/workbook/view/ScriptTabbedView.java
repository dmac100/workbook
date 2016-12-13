package workbook.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import workbook.event.MajorRefreshEvent;
import workbook.event.MinorRefreshEvent;
import workbook.event.ScriptTypeChangeEvent;
import workbook.model.Model;
import workbook.script.ScriptController;
import workbook.view.text.EditorText;

/**
 * A view that allows the editing and running of a script.
 */
public class ScriptTabbedView implements TabbedView {
	private final EditorText editorText;
	private final EventBus eventBus;
	private final ScriptController scriptController;
	private final Model model;
	
	public ScriptTabbedView(Composite parent, EventBus eventBus, ScriptController scriptController, Model model) {
		this.editorText = new EditorText(parent);
		this.eventBus = eventBus;
		this.scriptController = scriptController;
		this.model = model;
		
		editorText.getStyledText().addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if(event.keyCode == SWT.CR && event.stateMask == SWT.CONTROL) {
					refresh();
					event.doit = false;
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
	
	@Subscribe
	public void onMinorRefresh(MinorRefreshEvent event) {
	}
	
	@Subscribe
	public void onMajorRefresh(MajorRefreshEvent event) {
		refresh();
	}
	
	private void refresh() {
		Display.getDefault().asyncExec(() -> {
			scriptController.eval(editorText.getText())
				.thenRun(() -> eventBus.post(new MinorRefreshEvent(this)));
		});
	}

	private void refreshBrush() {
		Display.getDefault().asyncExec(() -> editorText.setBrush(model.getBrush()));
	}
	
	public Control getControl() {
		return editorText.getControl();
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