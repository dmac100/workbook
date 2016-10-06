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

import workbook.controller.MainController;
import workbook.editor.ui.HexTabbedEditor;
import workbook.editor.ui.StringTabbedEditor;
import workbook.editor.ui.TableTabbedEditor;
import workbook.editor.ui.TreeTabbedEditor;
import workbook.script.ScriptController.ScriptType;
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

public class MainView {
	private final Shell shell;
	private final MainController mainController;
	private final TabbedViewLayout tabbedViewLayout;
	private final TabbedViewFactory viewFactory;
	
	public MainView(Shell shell, MainController mainController) {
		this.shell = shell;
		
		shell.setLayout(new FillLayout());

		this.mainController = mainController;
		tabbedViewLayout = new TabbedViewLayout(shell);
		
		createMenuBar(shell);
		
		this.viewFactory = new TabbedViewFactory(tabbedViewLayout, mainController);
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
			.addItem("New Console").addSelectionListener(() -> viewFactory.addView(ConsoleTabbedView.class))
			.addItem("New Worksheet").addSelectionListener(() -> viewFactory.addView(WorksheetTabbedView.class))
			.addItem("New Script").addSelectionListener(() -> viewFactory.addView(ScriptTabbedView.class))
			.addItem("New Canvas").addSelectionListener(() -> viewFactory.addView(CanvasTabbedView.class))
			.addSeparator()
			.addItem("New String Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addView(StringTabbedEditor.class, expression)))
			.addItem("New Table Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addView(TableTabbedEditor.class, expression)))
			.addItem("New Tree Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addView(TreeTabbedEditor.class, expression)))
			.addItem("New Hex Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addView(HexTabbedEditor.class, expression)))
			.addSeparator()
			.addItem("Open...").addSelectionListener(() -> open())
			.addItem("Save...").addSelectionListener(() -> save())
			.addSeparator()
			.addItem("E&xit\tCtrl+Q").addSelectionListener(() -> shell.dispose());
		
		menuBuilder.addMenu("&Console")
			.addSubmenu("Engine", submenu -> submenu
				.addItem("Javascript").addSelectionListener(() -> mainController.setEngine(ScriptType.JAVASCRIPT))
				.addItem("Ruby").addSelectionListener(() -> mainController.setEngine(ScriptType.RUBY))
			)
			.addItem("Clear").addSelectionListener(() -> mainController.clearConsole());
		
		menuBuilder.addMenu("&Script")
			.addItem("Interrupt").addSelectionListener(() -> mainController.interrupt());
		
		menuBuilder.build();
	}
	
	private void getExpression(Consumer<String> consumer) {
		String expression = InputDialog.open(shell, "Expression", "Expression");
		if(expression != null && !expression.trim().isEmpty()) {
			consumer.accept(expression.trim());
		}
	}
	
	private void save() {
		String document = serialize();
		String location = selectSaveLocation();
		if(location != null) {
			try {
				FileUtils.writeStringToFile(new File(location), document, "UTF-8");
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void open() {
		open(selectOpenLocation());
	}
	
	private void save(String location) {
		save(selectOpenLocation());
	}
	
	private void open(String location) {
		if(location != null) {
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
