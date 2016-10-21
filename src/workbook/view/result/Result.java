package workbook.view.result;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import workbook.view.FontList;

/**
 * A view that displays the result of a script command.
 */
public class Result {
	private final Composite composite;
	private final ResultRenderer resultRenderer;

	public Result(Composite parent, ResultRenderer resultRenderer) {
		composite = new Composite(parent, SWT.NONE) {
			public Point computeSize(int wHint, int hHint, boolean changed) {
				Point size = super.computeSize(wHint, hHint, changed);
				return new Point(size.x, Math.min(size.y, 400));
			}
		};
		composite.setLayout(new FillLayout());
		
		this.resultRenderer = resultRenderer;
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