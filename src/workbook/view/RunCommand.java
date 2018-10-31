package workbook.view;

import java.util.ArrayList;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A dialog to let the user search for and run a command based on the given searchFunction.
 */
public class RunCommand extends Dialog {
	private Shell shell;
	private Text text;
	private List list;
	private String result;
	
	private Function<String, java.util.List<String>> searchFunction = text -> new ArrayList<String>();
	
	public RunCommand(Shell parent) {
		super(parent, SWT.NONE);
	}
	
	public String open() {
		GridLayout gridLayout = new GridLayout(1, false);
		
		gridLayout.verticalSpacing = 2;
		gridLayout.marginWidth = 2;
		gridLayout.marginHeight = 2;
		
		this.shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		shell.setLayout(gridLayout);
		shell.setSize(400, 300);
		shell.setText("Run Command");
		
		center(shell);
		
		this.text = new Text(shell, SWT.BORDER);
		this.list = new List(shell, SWT.BORDER | SWT.V_SCROLL);
		
		text.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		list.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent event) {
				selectCurrent();
			}
		});
		
		text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if(event.keyCode == SWT.ESC) {
					shell.dispose();
				} else if(event.keyCode == SWT.ARROW_UP) {
					scrollUp();
					event.doit = false;
				} else if(event.keyCode == SWT.ARROW_DOWN) {
					scrollDown();
					event.doit = false;
				} else if(event.keyCode == SWT.CR || event.keyCode == SWT.LF) {
					selectCurrent();
					event.doit = false;
				}
			}
		});
		
		list.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent event) {
				text.setFocus();
			}
		});
		
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				refreshList();
			}
		});
		
		shell.open();
		Display display = getParent().getDisplay();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		return result;
	}
	
	private void scrollUp() {
		if(list.getItemCount() > 0) {
			int index = list.getSelectionIndex();
			index--;
			if(index >= 0) {
				list.setSelection(index);
			} else {
				list.setSelection(0);
			}
		}
	}
	
	private void scrollDown() {
		if(list.getItemCount() > 0) {
			int index = list.getSelectionIndex();
			index++;
			if(index <= list.getItemCount() - 1) {
				list.setSelection(index);
			} else {
				list.setSelection(list.getItemCount() - 1);
			}
		}
	}
	
	private void selectCurrent() {
		if(list.getItemCount() > 0) {
			this.result = list.getItem((list.getSelectionIndex() >= 0) ? list.getSelectionIndex() : 0);
			shell.dispose();
		}
	}
	
	private void center(Shell dialog) {
        Rectangle bounds = getParent().getBounds();
        Point size = dialog.getSize();

        dialog.setLocation(
        	bounds.x + (bounds.width - size.x) / 2,
        	bounds.y + (bounds.height - size.y) / 2
        );
	}
	
	public void setSearchFunction(Function<String, java.util.List<String>> searchFunction) {
		this.searchFunction = searchFunction;
	}
	
	private void refreshList() {
		ArrayList<String> items = new ArrayList<>();
		
		for(String item:searchFunction.apply(text.getText())) {
			items.add(item);
		}
		
		list.setItems(items.toArray(new String[0]));
		
		if(list.getItemCount() > 0) {
			list.select(0);
		}
	}
}