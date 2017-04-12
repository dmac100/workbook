package workbook.view;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.jdom2.Element;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import workbook.editor.reference.OgnlReference;
import workbook.event.MajorRefreshEvent;
import workbook.event.MinorRefreshEvent;
import workbook.event.ScriptTypeChangeEvent;
import workbook.layout.GridDataBuilder;
import workbook.layout.GridLayoutBuilder;
import workbook.model.Model;
import workbook.script.ScriptController;
import workbook.util.ThrottledConsumer;

/**
 * A view that allows the editing and running of a script.
 */
public class BrowserTabbedView implements TabbedView {
	private final Composite composite;
	private final Browser browser;
	private final EventBus eventBus;
	private final ScriptController scriptController;
	private final Model model;
	
	private String previousUrlValue;
	private String previousHtmlValue;
	private String urlExpression;
	private String htmlExpression;
	
	private Consumer<Void> throttledReadValue = new ThrottledConsumer<>(1000, true, value -> readValue());
	
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
				if(browser.getUrl() != null && !browser.getUrl().equals("about:blank")) {
					browser.refresh();
				}
			}
		});
		
		location.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent event) {
				urlExpression = null;
				htmlExpression = null;
				setUrl(location.getText());
				previousUrlValue = location.getText();
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
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
	public void setUrl(String url) {
		if(url != null && !url.equals(previousUrlValue)) {
			browser.setUrl(url);
			previousUrlValue = url;
			previousHtmlValue = null;
		}
	}
	
	public void setHtml(String html) {
		if(html != null && !html.equals(previousHtmlValue)) {
			browser.setText(html);
			previousHtmlValue = html;
			previousUrlValue = null;
		}
	}
	
	@Subscribe
	public void onScriptTypeChange(ScriptTypeChangeEvent event) {
	}
	
	@Subscribe
	public void onMinorRefresh(MinorRefreshEvent event) {
		throttledReadValue.accept(null);
	}
	
	@Subscribe
	public void onMajorRefresh(MajorRefreshEvent event) {
		readValue();
	}

	public Control getControl() {
		return composite;
	}

	public void serialize(Element element) {
		if(this.urlExpression != null) {
			Element urlExpression = new Element("UrlExpression");
			urlExpression.setText(this.urlExpression);
			element.addContent(urlExpression);
		} else if(this.htmlExpression != null) {
			Element htmlExpression = new Element("HtmlExpression");
			htmlExpression.setText(this.htmlExpression);
			element.addContent(htmlExpression);
		} else {
			Element url = new Element("Url");
			url.setText(previousUrlValue);
			element.addContent(url);
		}
	}

	public void deserialize(Element element) {
		this.urlExpression = element.getChildText("UrlExpression");
		this.htmlExpression = element.getChildText("HtmlExpression");
		setUrl(element.getChildText("Url"));
	}
	
	public void createMenu(Menu menu) {
		MenuItem setUrlExpressionItem = new MenuItem(menu, SWT.NONE);
		setUrlExpressionItem.setText("Set URL Expression...");
		setUrlExpressionItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				String expression = InputDialog.open(Display.getCurrent().getActiveShell(), "Set URL Expression", "Expression", BrowserTabbedView.this.urlExpression);
				if(expression != null) {
					setUrlExpression(expression);
				}
			}
		});
		
		MenuItem setHtmlExpressionItem = new MenuItem(menu, SWT.NONE);
		setHtmlExpressionItem.setText("Set HTML Expression...");
		setHtmlExpressionItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				String expression = InputDialog.open(Display.getCurrent().getActiveShell(), "Set HTML Expression", "Expression", BrowserTabbedView.this.htmlExpression);
				if(expression != null) {
					setHtmlExpression(expression);
				}
			}
		});
	}
	
	private void setUrlExpression(String expression) {
		this.urlExpression = expression;
		this.htmlExpression = null;
		readValue();
	}
	
	private void setHtmlExpression(String expression) {
		this.htmlExpression = expression;
		this.urlExpression = null;
		readValue();
	}
	
	private void readValue() {
		if(urlExpression != null) {
			new OgnlReference(scriptController, urlExpression).get().thenAccept(value -> {
				String stringValue = (value == null) ? null : String.valueOf(value);
				Display.getDefault().asyncExec(() -> {
					setUrl(stringValue);
				});
			});
		} else if(htmlExpression != null) {
			new OgnlReference(scriptController, htmlExpression).get().thenAccept(value -> {
				String stringValue = (value == null) ? null : String.valueOf(value);
				Display.getDefault().asyncExec(() -> {
					setHtml(stringValue);
				});
			});
		} else {
			Display.getDefault().asyncExec(() -> {
				if(browser.getUrl() != null && !browser.getUrl().equals("about:blank")) {
					browser.refresh();
				}
			});
		}
	}
}