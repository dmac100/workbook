import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import controller.MainController;
import script.ScriptController.ScriptType;
import view.InputDialog;
import view.MenuBuilder;
import view.TabbedViewLayout;
import view.ViewFactory;

public class Workbook {
	private final Shell shell;
	private final MainController mainController;
	private final TabbedViewLayout tabbedViewLayout;
	private final ViewFactory viewFactory;
	
	public Workbook(Shell shell) {
		this.shell = shell;
		
		shell.setLayout(new FillLayout());

		mainController = new MainController();
		tabbedViewLayout = new TabbedViewLayout(shell);
		
		createMenuBar(shell);
		
		this.viewFactory = new ViewFactory(tabbedViewLayout, mainController);
		
		viewFactory.addWorksheet();
		viewFactory.addScript();
		viewFactory.addConsole();
		viewFactory.addTreeEditor("x");
		viewFactory.addCanvasView();
	}
	
	private void createMenuBar(final Shell shell) {
		MenuBuilder menuBuilder = new MenuBuilder(shell);
		
		menuBuilder.addMenu("&File")
			.addItem("New Console").addSelectionListener(() -> viewFactory.addConsole())
			.addItem("New Worksheet").addSelectionListener(() -> viewFactory.addWorksheet())
			.addItem("New Script").addSelectionListener(() -> viewFactory.addScript())
			.addItem("New Canvas").addSelectionListener(() -> viewFactory.addCanvasView())
			.addSeparator()
			.addItem("New String Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addStringEditor(expression)))
			.addItem("New Table Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addTableEditor(expression)))
			.addItem("New Tree Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addTreeEditor(expression)))
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
	
	private void deserialize(String documentText) throws JDOMException, IOException {
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

	public static void main(String[] args) {
		Display display = new Display();

		Shell shell = new Shell(display);
		
		Workbook main = new Workbook(shell);
		
		shell.setText("Workbook");
		shell.setSize(1000, 700);
		shell.open();

		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}

		display.dispose();
	}
}
