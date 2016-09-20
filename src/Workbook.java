import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import controller.MainController;
import view.MenuBuilder;
import view.TabbedView;

public class Workbook {
	private final Shell shell;
	private final MainController mainController;
	private final TabbedView tabbedView;
	private final ViewFactory viewFactory;
	
	public Workbook(Shell shell) {
		this.shell = shell;
		
		shell.setLayout(new FillLayout());

		mainController = new MainController();
		tabbedView = new TabbedView(shell);
		
		createMenuBar(shell);
		
		this.viewFactory = new ViewFactory(shell, tabbedView, mainController);
		
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
			.addItem("New String Editor...").addSelectionListener(() -> viewFactory.addStringEditor())
			.addItem("New Table Editor...").addSelectionListener(() -> viewFactory.addTableEditor())
			.addItem("New Tree Editor...").addSelectionListener(() -> viewFactory.addTreeEditor())
			.addSeparator()
			.addItem("Save").addSelectionListener(() -> save())
			.addSeparator()
			.addItem("E&xit\tCtrl+Q").addSelectionListener(() -> shell.dispose());
		
		menuBuilder.addMenu("&Script")
			.addItem("Interrupt").addSelectionListener(() -> mainController.interrupt());
		
		menuBuilder.build();
	}
	
	private void save() {
		String document = tabbedView.serialize();
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
