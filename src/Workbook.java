import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import view.CellList;
import view.MenuBuilder;

public class Workbook {
	private final Shell shell;
	
	public Workbook(Shell shell) {
		this.shell = shell;
		
		shell.setLayout(new FillLayout());
		
		CellList promptsList = new CellList(shell);
		
		createMenuBar(shell);
	}
	
	private void createMenuBar(final Shell shell) {
		MenuBuilder menuBuilder = new MenuBuilder(shell);
		
		menuBuilder.addMenu("&File")
			.addItem("E&xit\tCtrl+Q").addSelectionListener(() -> shell.dispose());
		
		menuBuilder.build();
	}
	
	public static void main(String[] args) {
		Display display = new Display();

		Shell shell = new Shell(display);

		Workbook main = new Workbook(shell);
		
		shell.setText("Workbook");
		shell.setSize(900, 600);
		shell.open();

		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}

		display.dispose();
	}
}
