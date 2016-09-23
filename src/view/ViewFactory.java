package view;

import java.util.function.Function;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;

import controller.MainController;
import editor.ScriptTableUtil;
import editor.ui.Editor;
import editor.ui.StringEditor;
import editor.ui.TableEditor;
import editor.ui.TreeEditor;
import view.canvas.CanvasView;

public class ViewFactory {
	private final TabbedViewLayout tabbedViewLayout;
	private final MainController mainController;
	private final ScriptTableUtil scriptTableUtil;
	
	public ViewFactory(TabbedViewLayout tabbedViewLayout, MainController mainController) {
		this.tabbedViewLayout = tabbedViewLayout;
		this.mainController = mainController;
		this.scriptTableUtil = new ScriptTableUtil(mainController.getScriptController());
	}
	
	public void addWorksheet() {
		addWorksheet(tabbedViewLayout.getLeftFolder(), "Worksheet");
	}
	
	public void addScript() {
		addScript(tabbedViewLayout.getLeftFolder(), "Script");
	}
	
	public void addConsole() {
		addConsole(tabbedViewLayout.getBottomFolder(), "Console");
	}
	
	public void addCanvasView() {
		addCanvasView(tabbedViewLayout.getRightFolder(), "Canvas");
	}
	
	public void addStringEditor(String expression) {
		StringEditor editor = addStringEditor(tabbedViewLayout.getRightFolder(), "Editor: " + expression);
		editor.setExpression(expression);
	}
	
	public void addTableEditor(String expression) {
		TableEditor editor = addTableEditor(tabbedViewLayout.getRightFolder(), "Editor: " + expression);
		editor.setExpression(expression);
	}
	
	public void addTreeEditor(String expression) {
		TreeEditor editor = addTreeEditor(tabbedViewLayout.getRightFolder(), "Editor: " + expression);
		editor.setExpression(expression);
	}
	
	public TabbedView addView(String type, CTabFolder folder, String title) {
		switch(type) {
			case "Worksheet": return addWorksheet(folder, title);
			case "ScriptEditor": return addScript(folder, title);
			case "Console": return addConsole(folder, title);
			case "CanvasView": return addCanvasView(folder, title);
			case "StringEditor": return addStringEditor(folder, title);
			case "TableEditor": return addTableEditor(folder, title);
			case "TreeEditor": return addTreeEditor(folder, title);
		}
		throw new IllegalArgumentException("Unknown type: " + type);
	}
	
	private Worksheet addWorksheet(CTabFolder folder, String title) {
		return tabbedViewLayout.addTab(folder, title, parent -> {
			Worksheet worksheet = new Worksheet(parent);
			mainController.addWorksheet(worksheet);
			return worksheet;
		});
	}
	
	private ScriptEditor addScript(CTabFolder folder, String title) {
		return tabbedViewLayout.addTab(folder, title, parent -> {
			ScriptEditor scriptEditor = new ScriptEditor(parent);
			mainController.addScriptEditor(scriptEditor);
			return scriptEditor;
		});
	}
	
	private Console addConsole(CTabFolder folder, String title) {
		return tabbedViewLayout.addTab(folder, title, parent -> {
			Console console = new Console(parent);
			mainController.addConsole(console);
			return console;
		});
	}
	
	private CanvasView addCanvasView(CTabFolder folder, String title) {
		return tabbedViewLayout.addTab(folder, title, parent -> {
			CanvasView canvas = new CanvasView(parent);
			mainController.addCanvasView(canvas);
			return canvas;
		});
	}
	
	private StringEditor addStringEditor(CTabFolder folder, String title) {
		return addEditor(folder, title, parent -> new StringEditor(parent));
	}
	
	private TableEditor addTableEditor(CTabFolder folder, String title) {
		return addEditor(folder, title, parent -> new TableEditor(parent, scriptTableUtil));
	}
	
	private TreeEditor addTreeEditor(CTabFolder folder, String title) {
		return addEditor(folder, title, parent -> new TreeEditor(parent, scriptTableUtil));
	}
	
	private <T extends Editor & TabbedView> T addEditor(CTabFolder folder, String title, Function<Composite, T> factory) {
		return tabbedViewLayout.addTab(folder, title, parent -> {
			T editor = factory.apply(parent);
			mainController.addEditor(editor);
			return editor;
		});
	}
}