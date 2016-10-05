package workbook.view.result;

import org.eclipse.swt.widgets.Composite;

public interface ResultRenderer {
	void addView(Composite parent, Object value, Runnable callback);
}
