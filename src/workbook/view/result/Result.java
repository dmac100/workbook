package workbook.view.result;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import workbook.view.FontList;

/**
 * A view that displays the result of a script command.
 */
public class Result {
	private final Composite composite;
	private final ResultRenderer resultRenderer;
	
	private Object previousValue = null;
	private boolean hasPreviousValue = false;

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
		styledText.setFont(FontList.MONO_NORMAL);
		styledText.setEditable(false);
		styledText.setText("...");
		styledText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
	}
	
	public void clear() {
		removeChildren();
	}
	
	public void setValue(Object value, Runnable callback) {
		composite.getDisplay().asyncExec(() -> {
			if(!composite.isDisposed()) {
				boolean changed = (hasPreviousValue && !Objects.equals(value, previousValue));
				this.previousValue = value;
				this.hasPreviousValue = true;
				
				removeChildren();
				
				resultRenderer.addView(composite, value, changed, callback);
				
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