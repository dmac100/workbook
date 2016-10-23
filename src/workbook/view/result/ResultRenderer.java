package workbook.view.result;

import org.eclipse.swt.widgets.Composite;

/**
 * Renders the results in a control.
 */
public interface ResultRenderer {
	/**
	 * Adds a view to a parent to display the value, and then runs callback.
	 */
	void addView(Composite parent, Object value, boolean changed, Runnable callback);
}
