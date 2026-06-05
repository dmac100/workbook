package workbook.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

class RectangleOverlay {
	private final Display display;
	private final Shell parentShell;
	private Shell overlay;
	
	private final List<Listener> moveListeners = new ArrayList<>();
	private final List<Runnable> doneCallbacks = new ArrayList<>();
	
	private Rectangle[] rectangles = {};
	
	public RectangleOverlay(Display display, Shell shell) {
		this.display = display;
		this.parentShell = shell;
	}

	public void setRectangles(Rectangle[] rectangles) {
		this.rectangles = rectangles;
		if(overlay != null) {
			overlay.redraw();
		}
	}

	public void addMoveListener(Listener moveListener) {
		moveListeners.add(moveListener);
		display.addFilter(SWT.MouseMove, moveListener);
	}
	
	public void addDoneCallback(Runnable doneCallback) {
		doneCallbacks.add(doneCallback);
	}

	public void setCursor(Cursor cursor) {
	}
	
	public void start() {
		this.overlay = new Shell(parentShell, SWT.NO_TRIM | SWT.ON_TOP);
		overlay.addListener(SWT.Paint, e -> {
			Point offset = overlay.toDisplay(0, 0);
			e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
			for(Rectangle rectangle:rectangles) {
				e.gc.drawRectangle(rectangle.x - offset.x, rectangle.y - offset.y, rectangle.width, rectangle.height);
			}
		});
		overlay.setAlpha(150);
		
		overlay.setLocation(parentShell.getLocation().x, parentShell.getLocation().y);
		overlay.setSize(parentShell.getSize().x, parentShell.getSize().y);
		overlay.open();
		
		Listener upListener = new Listener() {
			public void handleEvent(Event e) {
				display.removeFilter(SWT.MouseUp, this);
				
				moveListeners.forEach(moveListener -> display.removeFilter(SWT.MouseMove, moveListener));
				moveListeners.clear();
				
				doneCallbacks.forEach(Runnable::run);
				doneCallbacks.clear();
				
				if (!overlay.isDisposed()) {
					overlay.dispose();
				}
			}
		};

		display.addFilter(SWT.MouseUp, upListener);
	}
}
