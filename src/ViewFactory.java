import org.eclipse.swt.widgets.Shell;

import controller.MainController;
import editor.ScriptTableUtil;
import editor.ui.StringEditor;
import editor.ui.TableEditor;
import editor.ui.TreeEditor;
import view.CellList;
import view.Console;
import view.InputDialog;
import view.ScriptEditor;
import view.TabbedView;

public class ViewFactory {
	public final Shell shell;
	public final TabbedView tabbedView;
	public final MainController mainController;

	public ViewFactory(Shell shell, TabbedView tabbedView, MainController mainController) {
		this.shell = shell;
		this.tabbedView = tabbedView;
		this.mainController = mainController;
	}
	
	public void addWorksheet() {
		tabbedView.addTab(tabbedView.getLeftFolder(), "Worksheet", parent -> {
			CellList cellList = new CellList(parent);
			mainController.addCellList(cellList);
			return cellList;
		});
	}
	
	public void addScript() {
		tabbedView.addTab(tabbedView.getLeftFolder(), "Script", parent -> {
			ScriptEditor scriptEditor = new ScriptEditor(parent);
			mainController.addScriptEditor(scriptEditor);
			return scriptEditor;
		});
	}
	
	public void addConsole() {
		tabbedView.addTab(tabbedView.getBottomFolder(), "Console", parent -> {
			Console console = new Console(parent);
			mainController.addConsole(console);
			return console;
		});		
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
		tabbedView.addTab(tabbedView.getRightFolder(), "Editor: " + expression, parent -> {
			StringEditor stringEditor = new StringEditor(parent, expression);
			mainController.addEditor(stringEditor);
			return stringEditor;
		});
	}
	
	public void addTableEditor(String expression) {
		tabbedView.addTab(tabbedView.getRightFolder(), "Editor: " + expression, parent -> {
			ScriptTableUtil scriptTableUtil = new ScriptTableUtil(mainController.getScriptController());
			TableEditor tableEditor = new TableEditor(parent, expression, scriptTableUtil);
			mainController.addEditor(tableEditor);
			return tableEditor;
		});
	}
	
	public void addTreeEditor(String expression) {
		tabbedView.addTab(tabbedView.getRightFolder(), "Editor: " + expression, parent -> {
			ScriptTableUtil scriptTableUtil = new ScriptTableUtil(mainController.getScriptController());
			TreeEditor treeEditor = new TreeEditor(parent, expression, scriptTableUtil);
			mainController.addEditor(treeEditor);
			return treeEditor;
		});
	}
}
