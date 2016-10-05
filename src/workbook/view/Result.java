package workbook.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class Result {
	private final Composite composite;

	public Result(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
	}

	public void setLoading() {
		removeChildren();
		
		StyledText styledText = new StyledText(composite, SWT.NONE);
		styledText.setFont(FontList.consolas10);
		styledText.setEditable(false);
		styledText.setText("...");
	}

	public void setValue(Object value) {
		String valueString = String.valueOf(value);
		
		composite.getDisplay().asyncExec(() -> {
			if(!composite.isDisposed()) {
				removeChildren();
				
				StyledText styledText = new StyledText(composite, SWT.NONE);
				styledText.setFont(FontList.consolas10);
				styledText.setEditable(false);
				styledText.setText(valueString);
			}
		});
	}
	
	private void removeChildren() {
		for(Control control:composite.getChildren()) {
			control.dispose();
		}
	}

	public Composite asComposite() {
		return composite;
	}
}