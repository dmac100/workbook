package workbook.view.result;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import workbook.script.ScriptController;
import workbook.view.FontList;
import workbook.view.canvas.ColorCache;

/**
 * Renders results as a String.
 */
public class StringRenderer implements ResultRenderer {
	private final ResultRenderer next;
	private final ScriptController scriptController;
	private final ColorCache colorCache;

	public StringRenderer(ResultRenderer next, ScriptController scriptController) {
		this.next = next;
		this.scriptController = scriptController;
		this.colorCache = new ColorCache(Display.getCurrent());
	}
	
	public void addView(Composite parent, Object value, boolean changed, Runnable callback) {
		scriptController.exec(() -> {
			String valueString = String.valueOf(value);
			
			Display.getDefault().asyncExec(() -> {
				StyledText styledText = new StyledText(parent, SWT.NONE);
				styledText.setFont(FontList.consolas10);
				styledText.setEditable(false);
				styledText.setText(valueString);
				
				if(changed) {
					// Start animation to indicate value has changed.
					updateBackground(styledText, 0);
				}
				
				callback.run();
			});
			
			return null;
		});
	}

	/**
	 * Updates the background of the styledText to animate a transition from a color to white as t goes from 0 to 1.
	 */
	private void updateBackground(StyledText styledText, double t) {
		if(t <= 1) {
			Display.getDefault().timerExec(10, () -> {
				if(!styledText.isDisposed()) {
					double a = interpolate(t);
					int r = 255, g = 155, b = 115;
					styledText.setBackground(colorCache.getColor(
						(int) (255 - (255 - r) * (1 - a)),
						(int) (255 - (255 - g) * (1 - a)),
						(int) (255 - (255 - b) * (1 - a))
					));
					updateBackground(styledText, t + 0.015);
				}
			});
		}
	}

	/**
	 * Maps the time value from 0 to 1, to the target value from 0 to 1.
	 */
	private static double interpolate(double t) {
		double p = 0.5;
		return (t < p) ? 0 : (t - p) / (1 - p);
	}
}
