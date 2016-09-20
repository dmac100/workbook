import java.util.function.Function;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import controller.MainController;
import editor.ScriptTableUtil;
import editor.ui.Editor;
import editor.ui.StringEditor;
import editor.ui.TableEditor;
import editor.ui.TreeEditor;
import view.CellList;
import view.Console;
import view.InputDialog;
import view.ScriptEditor;
import view.TabbedView;
import view.View;

public class ViewFactory {
	private final Shell shell;
	private final TabbedView tabbedView;
	private final MainController mainController;
	private final ScriptTableUtil scriptTableUtil;

	public ViewFactory(Shell shell, TabbedView tabbedView, MainController mainController) {
		this.shell = shell;
		this.tabbedView = tabbedView;
		this.mainController = mainController;
		this.scriptTableUtil = new ScriptTableUtil(mainController.getScriptController());
	}
	
	public void addWorksheet() {
		addWorksheet(tabbedView.getLeftFolder(), "Worksheet");
	}
	
	public void addScript() {
		addScript(tabbedView.getLeftFolder(), "Script");
	}
	
	public void addConsole() {
		addConsole(tabbedView.getBottomFolder(), "Console");
	}
	
	public void addStringEditor() {
		String expression = InputDialog.open(shell, "Expression", "Expression");
		if(expression != null && !expression.trim().isEmpty()) {
			addStringEditor(expression.trim());
		}
	}
	
	public void addTableEditor() {
		String expression = InputDialog.open(shell, "Expression", "Expression");
		if(expression != null && !expression.trim().isEmpty()) {
			addTableEditor(expression.trim());
		}
	}
	
	public void addTreeEditor() {
		String expression = InputDialog.open(shell, "Expression", "Expression");
		if(expression != null && !expression.trim().isEmpty()) {
			addTreeEditor(expression.trim());
		}
	}
	
	public void addStringEditor(String expression) {
		StringEditor editor = addStringEditor(tabbedView.getRightFolder(), "Editor: " + expression);
		editor.setExpression(expression);
	}
	
	public void addTableEditor(String expression) {
		TableEditor editor = addTableEditor(tabbedView.getRightFolder(), "Editor: " + expression);
		editor.setExpression(expression);
	}
	
	public void addTreeEditor(String expression) {
		TreeEditor editor = addTreeEditor(tabbedView.getRightFolder(), "Editor: " + expression);
		editor.setExpression(expression);
	}
	
	public void addWorksheet(CTabFolder folder, String title) {
		tabbedView.addTab(folder, title, parent -> {
			CellList cellList = new CellList(parent);
			mainController.addCellList(cellList);
			return cellList;
		});
	}
	
	public void addScript(CTabFolder folder, String title) {
		tabbedView.addTab(folder, title, parent -> {
			ScriptEditor scriptEditor = new ScriptEditor(parent);
			mainController.addScriptEditor(scriptEditor);
			return scriptEditor;
		});
	}
	
	public void addConsole(CTabFolder folder, String title) {
		tabbedView.addTab(folder, title, parent -> {
			Console console = new Console(parent);
			mainController.addConsole(console);
			return console;
		});
	}
	
	public StringEditor addStringEditor(CTabFolder folder, String title) {
		return addEditor(folder, title, parent -> new StringEditor(parent));
	}
	
	public TableEditor addTableEditor(CTabFolder folder, String title) {
		return addEditor(folder, title, parent -> new TableEditor(parent, scriptTableUtil));
	}
	
	public TreeEditor addTreeEditor(CTabFolder folder, String title) {
		return addEditor(folder, title, parent -> new TreeEditor(parent, scriptTableUtil));
	}
	
	private <T extends Editor & View> T addEditor(CTabFolder folder, String title, Function<Composite, T> factory) {
		return tabbedView.addTab(folder, title, parent -> {
			T editor = factory.apply(parent);
			mainController.addEditor(editor);
			return editor;
		});
	}
}