package workbook.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.jdom2.Element;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import workbook.event.MajorRefreshEvent;
import workbook.event.MinorRefreshEvent;
import workbook.event.ScriptTypeChangeEvent;
import workbook.layout.GridDataBuilder;
import workbook.layout.GridLayoutBuilder;
import workbook.model.Model;
import workbook.script.NameAndProperties;
import workbook.util.ScrollUtil;
import workbook.view.text.EditorText;

class FormView {
	private final ScrolledComposite scrolledComposite;
	private final Composite composite;
	
	public FormView(Composite parent) {
		scrolledComposite = ScrollUtil.createScrolledComposite(parent);
		scrolledComposite.setLayout(new FillLayout());
		
		composite = (Composite) scrolledComposite.getContent();
		composite.setLayout(new GridLayoutBuilder().numColumns(2).build());
	}
	
	private void clearItems() {
		for(Control control:composite.getChildren()) {
			control.dispose();
		}
	}
	
	private void addSliderItem(String expression, String labelText, int min, int max) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(labelText + ":");
		label.setLayoutData(new GridDataBuilder().build());
		
		Composite control = new Composite(composite, SWT.NONE);
		control.setLayoutData(new GridDataBuilder().grabExcessHorizontalSpace(true).fillHorizontal().build());
		control.setLayout(new GridLayoutBuilder().numColumns(2).build());
		
		Slider slider = new Slider(control, SWT.NONE);
		slider.setMinimum(0);
		slider.setMaximum(max - min);
		slider.setThumb(1);
		slider.setSelection(0);
		slider.setLayoutData(new GridDataBuilder().fillHorizontal().build());
		
		Label value = new Label(control, SWT.NONE);
		value.setText(String.valueOf(min));
		value.setLayoutData(new GridDataBuilder().width(50).build());
		
		slider.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				value.setText(String.valueOf(slider.getSelection() + min));
			}
		});
	}
	
	private void addBooleanItem(String expression, String labelText) {
		Label label = new Label(composite, SWT.NONE);
		label.setText("");
		label.setLayoutData(new GridDataBuilder().build());
		
		Button button = new Button(composite, SWT.CHECK);
		button.setText(labelText);
		button.setLayoutData(new GridDataBuilder().grabExcessHorizontalSpace(true).build());
	}
	
	private void addTextItem(String expression, String labelText) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(labelText + ":");
		label.setLayoutData(new GridDataBuilder().build());

		Text text = new Text(composite, SWT.BORDER);
		text.setLayoutData(new GridDataBuilder().grabExcessHorizontalSpace(true).fillHorizontal().build());
	}
	
	private void addButton(String name, Runnable runnable) {
		Label label = new Label(composite, SWT.NONE);
		label.setText("");
		label.setLayoutData(new GridDataBuilder().build());

		Button button = new Button(composite, SWT.BORDER);
		button.setText(name);
	}
	
	public Control getControl() {
		return scrolledComposite;
	}

	public void setFormItems(List<NameAndProperties> values) {
		composite.getDisplay().asyncExec(() -> {
			clearItems();
			for(NameAndProperties nameAndProperties:values) {
				String name = nameAndProperties.getName();
				Map<String, String> properties = nameAndProperties.getProperties();
				
				if(name.equals("sliderItem")) {
					String expression = getStringOrDefault(properties.get("expression"), "expression");
					String label = getStringOrDefault(properties.get("label"), expression);
					double min = getDoubleOrDefault(properties.get("min"), 0);
					double max = getDoubleOrDefault(properties.get("max"), 100);
					addSliderItem(expression, label, (int) min, (int) max);
				} else if(name.equals("booleanItem")) {
					String expression = getStringOrDefault(properties.get("expression"), "expression");
					String label = getStringOrDefault(properties.get("label"), "label");
					addBooleanItem(expression, label);
				} else if(name.equals("textItem")) {
					String expression = getStringOrDefault(properties.get("expression"), "expression");
					String label = getStringOrDefault(properties.get("label"), "label");
					addTextItem(expression, label);
				}
			}
			composite.pack();
		});
	}
	
	private static double ensureInRange(double value, double min, double max) {
		if(value < min) return min;
		if(value > max) return max;
		return value;
	}

	private static double getDoubleOrDefault(String value, double defaultValue) {
		try {
			if(value != null) {
				return Double.parseDouble(value);
			}
		} catch(Exception e) {
		}
		
		return defaultValue;
	}

	private static String getStringOrDefault(String value, String defaultValue) {
		try {
			if(value != null) {
				return value;
			}
		} catch(Exception e) {
		}
		
		return defaultValue;
	}
}

/**
 * Displays a form view, that has an editor to write a script that renders a form, and
 * a form view.
 */
public class FormTabbedView implements TabbedView {
	private final TabFolder folder;
	private final EditorText editorText;
	private final Model model;
	
	private Consumer<String> executeCallback;
	
	private final List<FormView> formViews = new ArrayList<>();
	
	public FormTabbedView(Composite parent, EventBus eventBus, Model model) {
		folder = new TabFolder(parent, SWT.BOTTOM);
		this.model = model;
		
		TabItem designTab = new TabItem(folder, SWT.NONE);
		designTab.setText("Design");
		
		// Add design tab with form and editor.
		SashForm designSashForm = new SashForm(folder, SWT.NONE);
		this.editorText = new EditorText(designSashForm);
		designTab.setControl(designSashForm);
		FormView designTabFormView = new FormView(designSashForm);
		formViews.add(designTabFormView);
		
		// Add view tab with form only.
		TabItem viewTab = new TabItem(folder, SWT.NONE);
		viewTab.setText("View");
		FormView viewTabFormView = new FormView(folder);
		formViews.add(viewTabFormView);
		viewTab.setControl(viewTabFormView.getControl());
		
		editorText.getStyledText().addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if(event.keyCode == SWT.CR && event.stateMask == SWT.CONTROL) {
					refresh();
					event.doit = false;
				}
			}
		});
		
		folder.setSelection(1);
		
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
		if(event.getSource() != this) {
			refresh();
		}
	}
	
	@Subscribe
	public void onMajorRefresh(MajorRefreshEvent event) {
		refresh();
	}
	
	private void refreshBrush() {
		Display.getDefault().asyncExec(() -> editorText.setBrush(model.getBrush()));
	}
	
	public void setExecuteCallback(Consumer<String> executeCallback) {
		this.executeCallback = executeCallback;
	}
	
	public void refresh() {
		Display.getDefault().asyncExec(() -> {
			if(executeCallback != null) {
				executeCallback.accept(editorText.getText());
			}
		});
	}
	
	public void setFormItems(List<NameAndProperties> values) {
		formViews.forEach(formView -> formView.setFormItems(values));
	}
	
	public Control getControl() {
		return folder;
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
