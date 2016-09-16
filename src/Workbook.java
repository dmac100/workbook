import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import controller.MainController;
import editor.ScriptTableUtil;
import editor.ui.StringEditor;
import editor.ui.TableEditor;
import view.CellList;
import view.Console;
import view.InputDialog;
import view.MenuBuilder;
import view.TabbedView;

public class Workbook {
	private final Shell shell;
	private final MainController mainController;
	private final TabbedView tabbedView;
	
	public Workbook(Shell shell) {
		this.shell = shell;
		
		shell.setLayout(new FillLayout());

		mainController = new MainController();
		tabbedView = new TabbedView(shell);
		
		createMenuBar(shell);
		
		addWorksheet();
		addConsole();
		addTableEditor("x");
	}
	
	private void createMenuBar(final Shell shell) {
		MenuBuilder menuBuilder = new MenuBuilder(shell);
		
		menuBuilder.addMenu("&File")
			.addItem("New Console").addSelectionListener(() -> addConsole())
			.addItem("New Worksheet").addSelectionListener(() -> addWorksheet())
			.addSeparator()
			.addItem("New String Editor...").addSelectionListener(() -> addStringEditor())
			.addSeparator()
			.addItem("E&xit\tCtrl+Q").addSelectionListener(() -> shell.dispose());
		
		menuBuilder.addMenu("&Script")
			.addItem("Interrupt").addSelectionListener(() -> mainController.interrupt());
		
		menuBuilder.build();
	}
	
	private void addWorksheet() {
		tabbedView.addLeftTab("Worksheet", parent -> {
			CellList cellList = new CellList(parent);
			mainController.addCellList(cellList);
			return cellList.getControl();
		});
	}
	
	private void addConsole() {
		tabbedView.addBottomTab("Console", parent -> {
			Console console = new Console(parent);
			mainController.addConsole(console);
			return console.getControl();
		});		
	}
	
	private void addStringEditor() {
		String expression = InputDialog.open(shell, "Expression", "Expression");
		if(expression != null && !expression.trim().isEmpty()) {
			addStringEditor(expression.trim());
		}
	}
	
	private void addStringEditor(String expression) {
		tabbedView.addRightTab("Editor: " + expression, parent -> {
			StringEditor stringEditor = new StringEditor(parent, expression);
			mainController.addEditor(stringEditor);
			return stringEditor.getControl();
		});
	}
	
	private void addTableEditor(String expression) {
		tabbedView.addRightTab("Editor: " + expression, parent -> {
			ScriptTableUtil scriptTableUtil = new ScriptTableUtil(mainController.getScriptController());
			TableEditor tableEditor = new TableEditor(parent, expression, scriptTableUtil);
			mainController.addEditor(tableEditor);
			return tableEditor.getControl();
		});
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
