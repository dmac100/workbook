package workbook.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A dialog with a label and text field with OK/Cancel buttons.
 */
public class InputDialog extends Dialog {
	private String result;
	private String labelText;
	private String defaultValue;

	public InputDialog(Shell parent, String labelText, String defaultValue) {
		super(parent, 0);
		this.labelText = labelText;
		this.defaultValue = defaultValue;
	}
	
	public String open() {
		Shell parent = getParent();
		final Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		shell.setText(getText());
		shell.setLayout(new GridLayout());
		
		// Form Controls

		Composite formComposite = new Composite(shell, SWT.NONE);
		formComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout formLayout = new GridLayout(2, false);
		formComposite.setLayout(formLayout);
		
		Label label = new Label(formComposite, SWT.NONE);
		label.setText(labelText + ":");
		
		final Text text = new Text(formComposite, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		if(defaultValue != null) {
			text.setText(defaultValue);
			text.selectAll();
		}
		
		// Button Composite
		
		Composite buttonComposite = new Composite(shell, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
		
		GridLayout buttonLayout = new GridLayout(2, true);
		buttonLayout.marginHeight = 0;
		buttonComposite.setLayout(buttonLayout);
		
		Button okButton = new Button(buttonComposite, SWT.NONE);
		okButton.setText("OK");
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false));
		
		Button cancelButton = new Button(buttonComposite, SWT.NONE);
		cancelButton.setText("Cancel");
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false));

		shell.setDefaultButton(okButton);
		
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				result = text.getText();
				shell.dispose();
			}
		});
		
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				shell.dispose();
			}
		});
		
		// Open and wait for result.
		shell.setSize(300, 100);
		
		shell.open();
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}
	
	public static String open(Shell shell, String title, String prompt) {
		return open(shell, title, prompt, null);
	}
	
	public static String open(Shell shell, String title, String prompt, String defaultValue) {
		InputDialog dialog = new InputDialog(shell, prompt, defaultValue);
		dialog.setText(title);
		return dialog.open();
	}
}