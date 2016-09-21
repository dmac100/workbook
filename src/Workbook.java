import java.util.function.Consumer;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import controller.MainController;
import view.InputDialog;
import view.MenuBuilder;
import view.TabbedViewLayout;

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
	}
	
	private void createMenuBar(final Shell shell) {
		MenuBuilder menuBuilder = new MenuBuilder(shell);
		
		menuBuilder.addMenu("&File")
			.addItem("New Console").addSelectionListener(() -> viewFactory.addConsole())
			.addItem("New Worksheet").addSelectionListener(() -> viewFactory.addWorksheet())
			.addItem("New Script").addSelectionListener(() -> viewFactory.addScript())
			.addSeparator()
			.addItem("New String Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addStringEditor(expression)))
			.addItem("New Table Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addTableEditor(expression)))
			.addItem("New Tree Editor...").addSelectionListener(() -> getExpression(expression -> viewFactory.addTreeEditor(expression)))
			.addSeparator()
			.addItem("Save").addSelectionListener(() -> save())
			.addSeparator()
			.addItem("E&xit\tCtrl+Q").addSelectionListener(() -> shell.dispose());
		
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
		String document = tabbedViewLayout.serialize();
		System.out.println(document);
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
