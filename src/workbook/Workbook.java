package workbook;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jdom2.JDOMException;

import com.google.common.eventbus.EventBus;

import workbook.controller.MainController;
import workbook.editor.ui.ChartTabbedEditor;
import workbook.editor.ui.HexTabbedEditor;
import workbook.editor.ui.PolygonTabbedEditor;
import workbook.editor.ui.StringTabbedEditor;
import workbook.editor.ui.TableTabbedEditor;
import workbook.editor.ui.TreeTabbedEditor;
import workbook.event.MinorRefreshEvent;
import workbook.model.Model;
import workbook.script.ScriptFuture;
import workbook.view.BrowserTabbedView;
import workbook.view.ConsoleTabbedView;
import workbook.view.DependencyTabbedView;
import workbook.view.FormTabbedView;
import workbook.view.ScriptTabbedView;
import workbook.view.TabbedView;
import workbook.view.TabbedViewLayout.FolderPosition;
import workbook.view.WorksheetTabbedView;
import workbook.view.canvas.CanvasTabbedView;
import workbook.view.result.EditorRenderer;
import workbook.view.result.ResultRenderer;
import workbook.view.result.StringRenderer;
import workbook.view.result.TableRenderer;
import workbook.view.result.wrapper.ChartWrapper;

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
		System.setProperty("line.separator", "\n");
		
		eventBus = new EventBus();
		model = new Model();
		mainController = new MainController(eventBus, model);
		
		display = new Display();
		shell = new Shell(display);
		
		mainView = new MainView(shell, mainController, eventBus);
		
		registerViews();
		
		addDefaultViews();
		
		Map<String, Object> system = new HashMap<>();
		system.put("mainView", mainView);
		system.put("mainController", mainController);
		system.put("model", model);
		system.put("eventBus", eventBus);
		system.put("display", Display.getDefault());
		mainController.setVariable("system", system);
		
		mainController.registerWrapperFunction("chart", ChartWrapper::new);
		
		mainView.removeEmptyFolders();
	}
	
	private void registerViews() {
		ResultRenderer resultRenderer = createResultRenders();
		
		mainView.registerView(WorksheetTabbedView.class, "Worksheet", FolderPosition.LEFT, parent -> {
			return new WorksheetTabbedView(parent, eventBus, mainController.getScriptController(), resultRenderer);
		});
		
		mainView.registerView(ScriptTabbedView.class, "Script", FolderPosition.LEFT, parent -> {
			return new ScriptTabbedView(parent, eventBus, mainController.getScriptController(), model);
		});
		
		mainView.registerView(ConsoleTabbedView.class, "Console", FolderPosition.BOTTOM, parent -> {
			return new ConsoleTabbedView(parent, eventBus);
		});
		
		mainView.registerView(CanvasTabbedView.class, "Canvas", FolderPosition.RIGHT, parent -> {
			return new CanvasTabbedView(parent, eventBus, mainController.getScriptController(), model);
		});
		
		mainView.registerView(FormTabbedView.class, "Form", FolderPosition.RIGHT, parent -> {
			return new FormTabbedView(parent, eventBus, mainController.getScriptController(), model);
		});
		
		mainView.registerView(BrowserTabbedView.class, "Browser", FolderPosition.RIGHT, parent -> {
			return new BrowserTabbedView(parent, eventBus, mainController.getScriptController(), model);
		});
		
		mainView.registerView(DependencyTabbedView.class, "Dependencies", FolderPosition.RIGHT, parent -> {
			return new DependencyTabbedView(parent, eventBus, mainController.getScriptController(), model);
		});
		
		mainView.registerView(ChartTabbedEditor.class, "Chart Editor", FolderPosition.RIGHT, parent -> {
			return new ChartTabbedEditor(parent, eventBus, mainController.getScriptController());
		});
		
		mainView.registerView(StringTabbedEditor.class, "String Editor", FolderPosition.RIGHT, parent -> {
			return new StringTabbedEditor(parent, eventBus, mainController.getScriptController());
		});
		
		mainView.registerView(TableTabbedEditor.class, "Table Editor", FolderPosition.RIGHT, parent -> {
			return new TableTabbedEditor(parent, eventBus, mainController.getScriptController());
		});
		
		mainView.registerView(TreeTabbedEditor.class, "Tree Editor", FolderPosition.RIGHT, parent -> {
			return new TreeTabbedEditor(parent, eventBus, mainController.getScriptController());
		});
		
		mainView.registerView(HexTabbedEditor.class, "Hex Editor", FolderPosition.RIGHT, parent -> {
			return new HexTabbedEditor(parent, eventBus, mainController.getScriptController());
		});
		
		mainView.registerView(PolygonTabbedEditor.class, "Polygon Editor", FolderPosition.RIGHT, parent -> {
			return new PolygonTabbedEditor(parent, eventBus, mainController.getScriptController());
		});
	}
	
	private void addDefaultViews() {
		mainView.addView(WorksheetTabbedView.class);
	}

	private ResultRenderer createResultRenders() {
		ResultRenderer resultRenderer = null;
		
		resultRenderer = new StringRenderer(resultRenderer, mainController.getScriptController());
		resultRenderer = new TableRenderer(resultRenderer, mainController.getScriptController());
		
		resultRenderer = new EditorRenderer(resultRenderer, mainView, PolygonTabbedEditor::isPolygonList, PolygonTabbedEditor.class, 400);
		
		resultRenderer = new EditorRenderer(resultRenderer, mainView, value -> value instanceof ChartWrapper, ChartTabbedEditor.class, 250);
		
		resultRenderer = new EditorRenderer(resultRenderer, mainView);
		
		return resultRenderer;
	}
	
	public void addToolbarItem(String name, Runnable callback) {
		mainView.addToolbarItem(name, callback);
	}

	public void registerView(Class<? extends TabbedView> type, String defaultTitle, FolderPosition defaultPosition, Function<Composite, TabbedView> factory) {
		mainView.registerView(type, defaultTitle, defaultPosition, factory);
	}
	
	public void addView(Class<? extends TabbedView> type) {
		mainView.addView(type);
	}
	
	public ScriptFuture<Object> getVariable(String name) {
		return mainController.getVariable(name);
	}
	
	public void setVariable(String name, Object value) {
		mainController.setVariable(name, value);
	}
	
	public void refresh() {
		eventBus.post(new MinorRefreshEvent(this));
	}
	
	public void register(Class<? extends TabbedView> type, String defaultTitle, FolderPosition defaultPosition, Function<Composite, TabbedView> factory) {
		mainView.registerView(type, defaultTitle, defaultPosition, factory);
	}
	
	public void deserialize(String documentText) throws JDOMException, IOException {
		mainView.deserialize(documentText);
	}
	
	public void open(String location) {
		mainView.open(location);
	}

	public void waitForExit() {
		shell.setSize(800, 600);
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
		workbook.parseArgs(args);
		if(args.length == 1) {
			workbook.open(args[0]);
		}
		workbook.waitForExit();
		System.exit(0);
	}
	
	private void parseArgs(String[] args) {
		CommandLineParser parser = new GnuParser();
		
		Options options = new Options();
		options.addOption(new Option("l", "language", true, "set the language by name"));
		options.addOption(new Option("f", "file", true, "load a file"));
		options.addOption(new Option("h", "help", false, "show help"));
		
		try {
			CommandLine command = parser.parse(options, args);
		
			if(command.hasOption("h") || command.getArgs().length > 1) {
				new HelpFormatter().printHelp("java -jar workbook.jar [options] [filename]", options);
				System.exit(0);
			}
			
			if(command.hasOption("l")) {
				mainController.setEngine(command.getOptionValue("l"));
			}
			
			
			if(command.hasOption("f")) {
				open(command.getOptionValue("f"));
			}
			
			for(String s:command.getArgs()) {
				open(s);
			}
		} catch(Throwable e) {
			// Print usage and exit on any error.
			System.err.println(e.getMessage());
			new HelpFormatter().printHelp("java -jar workbook.jar", options);
			System.exit(0);
		}
	}
}
