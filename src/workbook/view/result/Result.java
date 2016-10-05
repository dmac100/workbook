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
	private final TableRenderer renderer;

	public Result(Composite parent, ScriptController scriptController) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
		
		StringRenderer stringRenderer = new StringRenderer(null, scriptController);
		TableRenderer tableRenderer = new TableRenderer(stringRenderer, scriptController);
		
		this.renderer = tableRenderer;
		
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
				
				renderer.addView(composite, value, callback);
				
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