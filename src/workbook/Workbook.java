package workbook;

import java.io.IOException;
import java.util.function.BiFunction;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jdom2.JDOMException;

import workbook.controller.MainController;
import workbook.editor.ui.HexTabbedEditor;
import workbook.editor.ui.StringTabbedEditor;
import workbook.editor.ui.TableTabbedEditor;
import workbook.editor.ui.TreeTabbedEditor;
import workbook.view.ConsoleTabbedView;
import workbook.view.ScriptTabbedView;
import workbook.view.TabbedView;
import workbook.view.TabbedViewLayout.FolderPosition;
import workbook.view.WorksheetTabbedView;
import workbook.view.canvas.CanvasTabbedView;
import workbook.view.result.ResultRenderer;
import workbook.view.result.StringRenderer;
import workbook.view.result.TableRenderer;

public class Workbook {
	private final MainController mainController;
	private final MainView mainView;
	
	private final Display display;
	private final Shell shell;
	
	public Workbook() {
		mainController = new MainController();
		
		display = new Display();
		shell = new Shell(display);
		
		mainView = new MainView(shell, mainController);
		
		ResultRenderer resultRenderer = createResultRenders();
		
		mainView.registerView(WorksheetTabbedView.class, "Worksheet", FolderPosition.LEFT, (controller, parent) -> controller.addWorksheet(new WorksheetTabbedView(parent, mainController.getScriptController(), resultRenderer)));
		mainView.registerView(ScriptTabbedView.class, "Script", FolderPosition.LEFT, (controller, parent) -> controller.addScriptEditor(new ScriptTabbedView(parent)));
		mainView.registerView(ConsoleTabbedView.class, "Console", FolderPosition.BOTTOM, (controller, parent) -> controller.addConsole(new ConsoleTabbedView(parent)));
		mainView.registerView(CanvasTabbedView.class, "Canvas", FolderPosition.RIGHT, (controller, parent) -> controller.addCanvasView(new CanvasTabbedView(parent)));
		mainView.registerView(StringTabbedEditor.class, "StringEditor", FolderPosition.RIGHT, (controller, parent) -> controller.addEditor(new StringTabbedEditor(parent)));
		mainView.registerView(TableTabbedEditor.class, "TableEditor", FolderPosition.RIGHT, (controller, parent) -> controller.addEditor(new TableTabbedEditor(parent, mainController.getScriptController())));
		mainView.registerView(TreeTabbedEditor.class, "TreeEditor", FolderPosition.RIGHT, (controller, parent) -> controller.addEditor(new TreeTabbedEditor(parent, mainController.getScriptController())));
		mainView.registerView(HexTabbedEditor.class, "HexEditor", FolderPosition.RIGHT, (controller, parent) -> controller.addEditor(new HexTabbedEditor(parent)));
		
		mainView.addView(WorksheetTabbedView.class);
		//mainView.addView(ScriptTabbedView.class);
		mainView.addView(ConsoleTabbedView.class);
		//mainView.addView(TreeTabbedEditor.class, "x");
		//mainView.addView(CanvasTabbedView.class);
		//mainView.addView(StringTabbedEditor.class, "x");
		//mainView.addView(HexTabbedEditor.class, "x");
		
		mainView.removeEmptyFolders();
	}
	
	private ResultRenderer createResultRenders() {
		ResultRenderer resultRenderer = null;
		
		resultRenderer = new StringRenderer(resultRenderer, mainController.getScriptController());
		resultRenderer = new TableRenderer(resultRenderer, mainController.getScriptController());
		
		return resultRenderer;
	}

	public void registerView(Class<? extends TabbedView> type, String defaultTitle, FolderPosition defaultPosition, BiFunction<MainController, Composite, TabbedView> factory) {
		mainView.registerView(type, defaultTitle, defaultPosition, factory);
	}
	
	public void addView(Class<? extends TabbedView> type) {
		mainView.addView(type);
	}
	
	public void addVariable(String name, Object value) {
		mainController.addVariable(name, value);
	}
	
	public void register(Class<? extends TabbedView> type, String defaultTitle, FolderPosition defaultPosition, BiFunction<MainController, Composite, TabbedView> factory) {
		mainView.registerView(type, defaultTitle, defaultPosition, factory);
	}
	
	public void deserialize(String documentText) throws JDOMException, IOException {
		mainView.deserialize(documentText);
	}

	public void waitForExit() {
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
	
	public static void main(String[] args) {
		Workbook workbook = new Workbook();
		workbook.waitForExit();
	}
}
