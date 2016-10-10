package workbook.view.result;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import workbook.script.ScriptController;
import workbook.view.FontList;

public class Result {
	private final ScriptController scriptController;
	private final Composite composite;
	private final ResultRenderer resultRenderer;

	public Result(Composite parent, ScriptController scriptController, ResultRenderer resultRenderer) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
		
		this.resultRenderer = resultRenderer;
		this.scriptController = scriptController;
	}
	
	public void setLoading() {
		removeChildren();
		
		StyledText styledText = new StyledText(composite, SWT.NONE);
		styledText.setFont(FontList.consolas10);
		styledText.setEditable(false);
		styledText.setText("...");
	}

	public void setValue(Object value, Runnable callback) {
		composite.getDisplay().asyncExec(() -> {
			if(!composite.isDisposed()) {
				removeChildren();
				
				resultRenderer.addView(composite, value, callback);
				
				composite.pack();
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