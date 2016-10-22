package workbook;

import java.io.IOException;
import java.util.function.BiFunction;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jdom2.JDOMException;

import com.google.common.eventbus.EventBus;

import workbook.controller.MainController;
import workbook.editor.ui.HexTabbedEditor;
import workbook.editor.ui.PolygonTabbedEditor;
import workbook.editor.ui.StringTabbedEditor;
import workbook.editor.ui.TableTabbedEditor;
import workbook.editor.ui.TreeTabbedEditor;
import workbook.model.Model;
import workbook.view.ConsoleTabbedView;
import workbook.view.ScriptTabbedView;
import workbook.view.TabbedView;
import workbook.view.TabbedViewLayout.FolderPosition;
import workbook.view.WorksheetTabbedView;
import workbook.view.canvas.CanvasTabbedView;
import workbook.view.result.ResultRenderer;
import workbook.view.result.StringRenderer;
import workbook.view.result.TableRenderer;

/**
 * Main entry point to this workbook. Creates the views and starts the workbook.
 */
public class Workbook {
	private final MainController mainController;
	private final EventBus eventBus;
	private final Model model;
	private final MainView mainView;
	
	private final Display display;
	private final Shell shell;
	
	public Workbook() {
		eventBus = new EventBus();
		model = new Model();
		mainController = new MainController(eventBus, model);
		
		display = new Display();
		shell = new Shell(display);
		
		mainView = new MainView(shell, mainController, eventBus);
		
		registerViews();
		
		mainView.addView(WorksheetTabbedView.class);
		//mainView.addView(ScriptTabbedView.class);
		mainView.addView(ConsoleTabbedView.class);
		//mainView.addView(TreeTabbedEditor.class, "x");
		//mainView.addView(CanvasTabbedView.class);
		//mainView.addView(StringTabbedEditor.class, "x");
		//mainView.addView(HexTabbedEditor.class, "x");
		
		mainView.removeEmptyFolders();
	}
	
	private void registerViews() {
		ResultRenderer resultRenderer = createResultRenders();
		
		mainView.registerView(WorksheetTabbedView.class, "Worksheet", FolderPosition.LEFT, (controller, parent) -> {
			return controller.addWorksheet(new WorksheetTabbedView(parent, eventBus, resultRenderer));
		});
		
		mainView.registerView(ScriptTabbedView.class, "Script", FolderPosition.LEFT, (controller, parent) -> {
			return controller.addScriptEditor(new ScriptTabbedView(parent, eventBus, model));
		});
		
		mainView.registerView(ConsoleTabbedView.class, "Console", FolderPosition.BOTTOM, (controller, parent) -> {
			return controller.addConsole(new ConsoleTabbedView(parent, eventBus));
		});
		
		mainView.registerView(CanvasTabbedView.class, "Canvas", FolderPosition.RIGHT, (controller, parent) -> {
			return controller.addCanvasView(new CanvasTabbedView(parent, eventBus, model));
		});
		
		mainView.registerView(StringTabbedEditor.class, "StringEditor", FolderPosition.RIGHT, (controller, parent) -> {
			return controller.addEditor(new StringTabbedEditor(parent, eventBus));
		});
		
		mainView.registerView(TableTabbedEditor.class, "TableEditor", FolderPosition.RIGHT, (controller, parent) -> {
			return controller.addEditor(new TableTabbedEditor(parent, eventBus, mainController.getScriptController()));
		});
		
		mainView.registerView(TreeTabbedEditor.class, "TreeEditor", FolderPosition.RIGHT, (controller, parent) -> {
			return controller.addEditor(new TreeTabbedEditor(parent, eventBus, mainController.getScriptController()));
		});
		
		mainView.registerView(HexTabbedEditor.class, "HexEditor", FolderPosition.RIGHT, (controller, parent) -> {
			return controller.addEditor(new HexTabbedEditor(parent, eventBus));
		});
		
		mainView.registerView(PolygonTabbedEditor.class, "PolygonEditor", FolderPosition.RIGHT, (controller, parent) -> {
			return controller.addEditor(new PolygonTabbedEditor(parent, eventBus));
		});
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
			try {
				if(!display.readAndDispatch()) {
					display.sleep();
				}
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}

		display.dispose();
	}
	
	public static void main(String[] args) {
		Workbook workbook = new Workbook();
		if(args.length == 1) {
			workbook.mainView.open(args[0]);
		}
		workbook.waitForExit();
	}
}
