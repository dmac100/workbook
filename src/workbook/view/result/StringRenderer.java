package workbook.view.result;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;

import com.google.common.base.Throwables;

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
			String valueString;
			boolean red;
			
			if(value instanceof Throwable) {
				valueString = Throwables.getStackTraceAsString((Throwable) value);
				red = true;
			} else {
				valueString = WordUtils.wrap(String.valueOf(value), 1000, "\n", true);
				red = false;
			}
			
			Display.getDefault().asyncExec(() -> {
				// Remove any existing results.
				for(Control control:parent.getChildren()) {
					control.dispose();
				}
				
				StyledText styledText = createStyledText(parent);
				styledText.setFont(FontList.MONO_NORMAL);
				styledText.setEditable(false);
				styledText.setText(valueString);
				styledText.setAlwaysShowScrollBars(false);
				
				if(red) {
					StyleRange styleRange = new StyleRange();
					styleRange.start = 0;
					styleRange.length = styledText.getCharCount();
					styleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
					styledText.replaceStyleRanges(0, styledText.getCharCount(), new StyleRange[] { styleRange });
				}
				
				// Remove selection after losing focus.
				styledText.addFocusListener(new FocusAdapter() {
					public void focusLost(FocusEvent event) {
						styledText.setSelection(styledText.getSelection().x);
					}
				});
				
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
	 * Creates a StyledText that disables the refreshing of the scrollbars when setting the background.
	 * This causes flickering when animating the background color on Windows.
	 */
	private static StyledText createStyledText(Composite parent) {
		return new StyledText(parent, SWT.V_SCROLL) {
			boolean settingBackground = false;
			
			public void setBackground(Color color) {
				settingBackground = true;
				super.setBackground(color);
				settingBackground = false;
			}
			
			public ScrollBar getVerticalBar() {
				return settingBackground ? null : super.getVerticalBar();
			}
		};
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
		} else {
			styledText.setBackground(colorCache.getColor(255, 255, 255));
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
