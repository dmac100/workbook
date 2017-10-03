package workbook.view;

import java.util.ArrayList;
import java.util.List;

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
 * A view that allows the editing of a list of dependencies for the program.
 */
public class DependencyTabbedView implements TabbedView {
	private final EditorText editorText;
	private final EventBus eventBus;
	private final ScriptController scriptController;
	private final Model model;
	
	public DependencyTabbedView(Composite parent, EventBus eventBus, ScriptController scriptController, Model model) {
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
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
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
			String text = editorText.getText();
			List<String> dependencies = new ArrayList<>();
			for(String line:text.split("\n")) {
				dependencies.add(line.trim());
			}
			IvyDownloader.downloadDependencies(dependencies);
		});
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