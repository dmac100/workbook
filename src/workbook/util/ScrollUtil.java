package workbook.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

public class ScrollUtil {
	/**
	 * Creates a ScrolledComposite containing a composite that will be resized when the scrolled composite is resized.
	 */
	public static ScrolledComposite createScrolledComposite(Composite parent) {
		final ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		final Composite composite = new Composite(scrolledComposite, SWT.NONE);
		scrolledComposite.setContent(composite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent event) {
				scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});
		return scrolledComposite;
	}
	
	/**
	 * Scrolls a ScrolledComposite so that bounds is visible.
	 */
	public static void scrollTo(ScrolledComposite scrolledComposite, Rectangle bounds) {
		scrollTo(scrolledComposite, bounds, true, true);
	}
	
	/**
	 * Scrolls a ScrolledComposite horizontally so that bounds is visible.
	 */
	public static void scrollHorizontallyTo(ScrolledComposite scrolledComposite, Rectangle bounds) {
		scrollTo(scrolledComposite, bounds, true, false);
	}
	
	/**
	 * Scrolls a ScrolledComposite vertically so that bounds is visible.
	 */
	public static void scrollVerticallyTo(ScrolledComposite scrolledComposite, Rectangle bounds) {
		scrollTo(scrolledComposite, bounds, false, true);
	}
	
	private static void scrollTo(ScrolledComposite scrolledComposite, Rectangle bounds, boolean scrollHorizontally, boolean scrollVertically) {
		boolean scroll = false;
		Rectangle area = scrolledComposite.getClientArea();
		Point origin = scrolledComposite.getOrigin();
		if(scrollHorizontally) {
			if(origin.x > bounds.x) {
				origin.x = Math.max(0, bounds.x);
				scroll = true;
			}
			if(origin.x + area.width < bounds.x + bounds.width) {
				origin.x = Math.max(0, bounds.x + bounds.width - area.width);
				scroll = true;
			}
		}
		if(scrollVertically) {
			if(origin.y + area.height < bounds.y + bounds.height) {
				origin.y = Math.max(0, bounds.y + bounds.height - area.height);
				scroll = true;
			}
			if(origin.y > bounds.y) {
				origin.y = Math.max(0, bounds.y);
				scroll = true;
			}
		}
		if(scroll) {
			scrolledComposite.setOrigin(origin);
		}
	}
}
