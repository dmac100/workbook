package workbook.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.jdom2.Element;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import workbook.event.MajorRefreshEvent;
import workbook.event.MinorRefreshEvent;
import workbook.event.ScriptTypeChangeEvent;
import workbook.layout.FillLayoutBuilder;
import workbook.layout.GridDataBuilder;
import workbook.layout.GridLayoutBuilder;
import workbook.model.Model;
import workbook.script.ScriptController;

/**
 * A view that allows the editing and running of a script.
 */
public class BrowserTabbedView implements TabbedView {
	private final Composite composite;
	private final Browser browser;
	private final EventBus eventBus;
	private final ScriptController scriptController;
	private final Model model;
	
	public BrowserTabbedView(Composite parent, EventBus eventBus, ScriptController scriptController, Model model) {
		this.eventBus = eventBus;
		this.scriptController = scriptController;
		this.model = model;
		
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayoutBuilder().numColumns(1).marginWidth(0).marginHeight(0).verticalSpacing(0).build());
		
		Composite toolbar = new Composite(composite, SWT.NONE);
		toolbar.setLayout(new GridLayoutBuilder().numColumns(5).marginHeight(0).marginWidth(5).marginTop(3).marginBottom(3).build());
		toolbar.setLayoutData(new GridDataBuilder().fillHorizontal().build());
		
		Button backButton = new Button(toolbar, SWT.NONE);
		backButton.setText("<");
		
		Button forwardButton = new Button(toolbar, SWT.NONE);
		forwardButton.setText(">");
		
		Button stopButton = new Button(toolbar, SWT.NONE);
		stopButton.setText("Stop");
		
		Button reloadButton = new Button(toolbar, SWT.NONE);
		reloadButton.setText("Reload");
		
		Text location = new Text(toolbar, SWT.BORDER);
		location.setLayoutData(new GridDataBuilder().fillHorizontal().build());
		
		browser = new Browser(composite, SWT.NONE);
		browser.setLayoutData(new GridDataBuilder().fillHorizontal().fillVertical().build());
		
		backButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				browser.back();
			}
		});
		
		forwardButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				browser.forward();
			}
		});
		
		stopButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				browser.stop();
			}
		});
		
		reloadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				browser.setUrl(browser.getUrl());
			}
		});
		
		location.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent event) {
				browser.setUrl(location.getText());
			}
		});
		
		browser.addLocationListener(new LocationListener() {
			public void changed(LocationEvent event) {
				backButton.setEnabled(browser.isBackEnabled());
				forwardButton.setEnabled(browser.isForwardEnabled());
				stopButton.setEnabled(false);
				location.setText(browser.getUrl());
			}

			public void changing(LocationEvent event) {
				stopButton.setEnabled(true);
			}
		});
		
		setUrl("http://www.google.com/");
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
	public void setUrl(String url) {
		browser.setUrl(url);
	}
	
	public void setHtml(String html) {
		browser.setText(html);
	}
	
	@Subscribe
	public void onScriptTypeChange(ScriptTypeChangeEvent event) {
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
			if(!browser.getUrl().equals("about:blank")) {
				browser.refresh();
			}
		});
	}

	public Control getControl() {
		return composite;
	}

	public void serialize(Element element) {
		Element url = new Element("URL");
		url.setText(browser.getUrl());
		element.addContent(url);
	}

	public void deserialize(Element element) {
		String url = element.getChildText("URL");
		if(url != null) {
			browser.setUrl(url);
		}
	}
}