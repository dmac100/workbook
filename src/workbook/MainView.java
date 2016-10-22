package workbook;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

import workbook.controller.MainController;
import workbook.editor.ui.HexTabbedEditor;
import workbook.editor.ui.PolygonTabbedEditor;
import workbook.editor.ui.StringTabbedEditor;
import workbook.editor.ui.TableTabbedEditor;
import workbook.editor.ui.TreeTabbedEditor;
import workbook.event.MajorRefreshEvent;
import workbook.script.Engine;
import workbook.script.GroovyEngine;
import workbook.script.JavascriptEngine;
import workbook.script.RubyEngine;
import workbook.view.ConsoleTabbedView;
import workbook.view.InputDialog;
import workbook.view.MenuBuilder;
import workbook.view.ScriptTabbedView;
import workbook.view.TabbedView;
import workbook.view.TabbedViewFactory;
import workbook.view.TabbedViewLayout;
import workbook.view.TabbedViewLayout.FolderPosition;
import workbook.view.WorksheetTabbedView;
import workbook.view.canvas.CanvasTabbedView;

/**
 * Main view for the workbook that contains all the controls of this program.
 */
public class MainView {
	private final Shell shell;
	private final MainController mainController;
	private final EventBus eventBus;
	private final TabbedViewLayout tabbedViewLayout;
	private final TabbedViewFactory viewFactory;
	
	private String currentFileLocation = null;
	
	public MainView(Shell shell, MainController mainController, EventBus eventBus) {
		this.shell = shell;
		this.eventBus = eventBus;
		
		shell.setLayout(new FillLayout());

		this.mainController = mainController;
		tabbedViewLayout = new TabbedViewLayout(shell);
		
		createMenuBar(shell);
		
		this.viewFactory = new TabbedViewFactory(tabbedViewLayout, mainController);
		
		registerEngine("Javascript", JavascriptEngine::new);
		registerEngine("Ruby", RubyEngine::new);
		registerEngine("Groovy", GroovyEngine::new);
		
		mainController.setEngine("Groovy");
	}
	
	private void registerEngine(String name, Supplier<Engine> engineSupplier) {
		try {
			mainController.registerEngine(name, engineSupplier.get());
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	public void registerView(Class<? extends TabbedView> type, String defaultTitle, FolderPosition defaultPosition, BiFunction<MainController, Composite, TabbedView> factory) {
		viewFactory.registerView(type, defaultTitle, defaultPosition, factory);
	}
	
	public TabbedView addView(Class<? extends TabbedView> type) {
		return viewFactory.addView(type);
	}
	
	public TabbedView addView(Class<? extends TabbedView> type, String expression) {
		return viewFactory.addView(type, expression);
	}
	
	public void removeEmptyFolders() {
		tabbedViewLayout.removeEmptyFolders();
	}
	
	private void createMenuBar(final Shell shell) {
		MenuBuilder menuBuilder = new MenuBuilder(shell);
		
		menuBuilder.addMenu("&File")
			.addItem("Open...\tCtrl+O").addSelectionListener(() -> open()).setAccelerator(SWT.CONTROL | 'o')
			.addSeparator()
			.addItem("Save\tCtrl+S").addSelectionListener(() -> save()).setAccelerator(SWT.CONTROL | 's')
			.addItem("Save As...\tCtrl+Shift+S").addSelectionListener(() -> saveAs()).setAccelerator(SWT.CONTROL | SWT.SHIFT | 's')
			.addSeparator()
			.addItem("E&xit\tCtrl+Q").addSelectionListener(() -> shell.dispose()).setAccelerator(SWT.CONTROL | 'q');
		
		
		menuBuilder.addMenu("&Editors")
			.addItem("New Console").addSelectionListener(() -> viewFactory.addView(ConsoleTabbedView.class))
			.addItem("New Worksheet").addSelectionListener(() -> viewFactory.addView(WorksheetTabbedView.class))
			.addItem("New Script").addSelectionListener(() -> viewFactory.addView(ScriptTabbedView.class))
			.addItem("New Canvas").addSelectionListener(() -> viewFactory.addView(CanvasTabbedView.class))
			.addSeparator()
			.addItem("New String Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addView(StringTabbedEditor.class, expression)))
			.addItem("New Table Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addView(TableTabbedEditor.class, expression)))
			.addItem("New Tree Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addView(TreeTabbedEditor.class, expression)))
			.addItem("New Hex Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addView(HexTabbedEditor.class, expression)))
			.addItem("New Polygon Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addView(PolygonTabbedEditor.class, expression)));
		
		menuBuilder.addMenu("&Script")
			.addItem("Refresh All\tCtrl+Shift+Enter").addSelectionListener(() -> eventBus.post(new MajorRefreshEvent())).setAccelerator(SWT.CONTROL | SWT.SHIFT | '\r')
			.addItem("Interrupt").addSelectionListener(() -> mainController.interrupt())
			.addSeparator()
			.addSubmenu("Engine", submenu -> submenu
				.addItem("Javascript").addSelectionListener(() -> mainController.setEngine("Javascript"))
				.addItem("Ruby").addSelectionListener(() -> mainController.setEngine("Ruby"))
				.addItem("Groovy").addSelectionListener(() -> mainController.setEngine("Groovy"))
			);
		
		menuBuilder.build();
	}
	
	private void getExpression(Consumer<String> consumer) {
		String expression = InputDialog.open(shell, "Expression", "Expression");
		if(expression != null && !expression.trim().isEmpty()) {
			consumer.accept(expression.trim());
		}
	}
	
	private void save() {
		if(currentFileLocation == null) {
			saveAs();
		} else {
			save(currentFileLocation);
		}
	}
	
	private void saveAs() {
		String location = selectSaveLocation();
		if(location != null) {
			currentFileLocation = location;
			save(location);
		}
	}
	
	public void save(String location) {
		String document = serialize();
		
		try {
			FileUtils.writeStringToFile(new File(location), document, "UTF-8");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void open() {
		open(selectOpenLocation());
	}
	
	public void open(String location) {
		if(location != null) {
			currentFileLocation = location;
			
			try {
				String document = FileUtils.readFileToString(new File(location), "UTF-8");
				mainController.clear();
				tabbedViewLayout.clear();
				deserialize(document);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private String serialize() {
		Document document = new Document();
		
		Element workbookElement = new Element("Workbook");
		document.addContent(workbookElement);
		
		Element tabsElement = new Element("Tabs");
		workbookElement.addContent(tabsElement);
		tabbedViewLayout.serialize(tabsElement);
		
		Element controllerElement = new Element("Controller");
		workbookElement.addContent(controllerElement);
		mainController.serialize(controllerElement);
		
		return new XMLOutputter(Format.getPrettyFormat()).outputString(document);
	}
	
	public void deserialize(String documentText) throws JDOMException, IOException {
		Document document = new SAXBuilder().build(new StringReader(documentText));
		
		Element workbookElement = document.getRootElement().getChild("Workbook");
		
		Element tabsElement = document.getRootElement().getChild("Tabs");
		tabbedViewLayout.deserialize(viewFactory, tabsElement);
		
		Element controllerElement = document.getRootElement().getChild("Controller");
		mainController.deserialize(controllerElement);
	}

	private String selectSaveLocation() {
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setText("Save");
		dialog.setFileName("book.wb");
		return dialog.open();
	}
	
	private String selectOpenLocation() {
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setText("Open");
		dialog.setFilterExtensions(new String[] { "*.wb", "*.*" });
		
		return dialog.open();
	}
	
	private void displayException(Exception e) {
		MessageBox messageBox = new MessageBox(shell);
		messageBox.setText("Error");
		messageBox.setMessage(e.getMessage() == null ? e.toString() : e.getMessage());
		e.printStackTrace();
		
		messageBox.open();
	}
}
