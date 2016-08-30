import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import view.CellList;

public class Workbook {
	private final Shell shell;
	
	public Workbook(Shell shell) {
		this.shell = shell;
		
		shell.setLayout(new FillLayout());
		
		CellList promptsList = new CellList(shell);
	}
	
	public static void main(String[] args) {
		Display display = new Display();

		Shell shell = new Shell(display);

		Workbook main = new Workbook(shell);
		
		shell.setSize(400, 300);
		shell.open();

		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}

		display.dispose();
	}
}
