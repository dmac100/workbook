package workbook.editor.ui;

import static workbook.util.TypeUtil.isListOf;
import static workbook.util.TypeUtil.isListOfOrEmpty;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.google.common.eventbus.EventBus;

import workbook.event.MinorRefreshEvent;
import workbook.script.ScriptController;
import workbook.view.TabbedView;

class PolygonCanvas {
	private interface Tool {
		public default void mouseMoved(MouseEvent event) {}
		public default void mousePressed(MouseEvent event) {}
		public default void mouseDragged(MouseEvent event) {}
		public default void mouseReleased(MouseEvent event) {}
		public default void mouseDoubleClicked(MouseEvent event) {}
	}
	
	/**
	 * Adds a point to a polygon on click, and completes the polygon on double-click.
	 */
	private class AddPolygonTool implements Tool {
		private boolean addingPolygon = false;
		
		public void mouseReleased(MouseEvent event) {
			if(!addingPolygon) {
				polygons.add(new ArrayList<>());
				getLastPolygon().add(new Point(event.x, event.y));
				addingPolygon = true;
			}

			getLastPolygon().add(new Point(event.x, event.y));
		}
		
		public void mouseDoubleClicked(MouseEvent event) {
			addingPolygon = false;
		}

		public void mouseMoved(MouseEvent event) {
			if(addingPolygon) {
				Point point = new Point(event.x, event.y);
				List<Point> polygon = getLastPolygon();
				polygon.set(polygon.size() - 1, point);
				
				firePolygonChanged();
			}
		}
	}
	
	/**
	 * Removes the nearest polygon on click.
	 */
	private class RemovePolygonTool implements Tool {
		private int polygonIndex = -1;
		
		public void mousePressed(MouseEvent event) {
			int x = event.x;
			int y = event.y;
			
			findRemovePolygon(x, y);
			
			if(polygonIndex != -1) {
				polygons.remove(polygonIndex);
				polygonIndex = -1;
				
				firePolygonChanged();
			}
		}

		/**
		 * Sets polygonIndex to the nearest polygon.
		 */
		private void findRemovePolygon(int x, int y) {
			double minDistance = Double.MAX_VALUE;
			
			for(int i = 0; i < polygons.size(); i++) {
				List<Point> polygon = polygons.get(i);
				for(int j = 0; j < polygon.size(); j++) {
					Point point1 = polygon.get(j);
					Point point2 = polygon.get((j + 1) % polygon.size());
					
					double distance = distanceToLine(new Point(x, y), point1, point2);
					if(distance < minDistance) {
						polygonIndex = i;
						minDistance = distance;
					}
				}
			}
		}
	}

	/**
	 * Moves points when dragged, inserts points when clicked next to a line.
	 */
	private class ModifyPointsTool implements Tool {
		private int polygonIndex = -1;
		private int pointIndex = -1;
		
		public void mousePressed(MouseEvent event) {
			int x = event.x;
			int y = event.y;
			
			if(event.button == 1) {
				polygonIndex = -1;
				
				findMovePoint(x, y);
				
				if(polygonIndex == -1) {
					findInsertPoint(x, y);
					
					if(polygonIndex != -1) {
						// Add new point where clicked.
						List<Point> polygon = polygons.get(polygonIndex);
						polygon.add(pointIndex, new Point(x, y));
						
						firePolygonChanged();
					}
				}
			} else if(event.button == 3) {
				findDeletePoint(x, y);
				
				if(polygonIndex != -1) {
					List<Point> polygon = polygons.get(polygonIndex);
					// Remove nearest point on click.
					polygon.remove(pointIndex);
					
					// Remove polygon is now empty.
					if(polygon.size() == 0) {
						polygons.remove(polygonIndex);
					}
					
					polygonIndex = -1;
					
					firePolygonChanged();
				}
			}
		}
		
		/**
		 * Sets polygonIndex and pointIndex to any point at the given location.
		 */
		private void findMovePoint(int x, int y) {
			for(int i = 0; i < polygons.size(); i++) {
				List<Point> polygon = polygons.get(i);
				for(int j = 0; j < polygon.size(); j++) {
					Point point = polygon.get(j);
				
					if(distance(point, new Point(x, y)) < 20) {
						polygonIndex = i;
						pointIndex = j;
					}
				}
			}
		}
		
		/**
		 * Sets polygonIndex and pointIndex to point next to the nearest line.
		 */
		private void findInsertPoint(int x, int y) {
			double minDistance = Double.MAX_VALUE;
			
			for(int i = 0; i < polygons.size(); i++) {
				List<Point> polygon = polygons.get(i);
				for(int j = 0; j < polygon.size(); j++) {
					Point point1 = polygon.get(j);
					Point point2 = polygon.get((j + 1) % polygon.size());
					
					double distance = distanceToLine(new Point(x, y), point1, point2);
					if(distance < minDistance) {
						polygonIndex = i;
						pointIndex = (j + 1) % polygon.size();
						minDistance = distance;
					}
				}
			}
		}
		
		/**
		 * Sets polygonIndex and pointIndex to the nearest point.
		 */
		private void findDeletePoint(int x, int y) {
			double minDistance = Double.MAX_VALUE;
			
			for(int i = 0; i < polygons.size(); i++) {
				List<Point> polygon = polygons.get(i);
				for(int j = 0; j < polygon.size(); j++) {
					Point point = polygon.get(j);
					
					double distance = distance(new Point(x, y), point);
					
					if(distance < minDistance) {
						polygonIndex = i;
						pointIndex = j;
						minDistance = distance;
					}
				}
			}
		}
		
		public void mouseDragged(MouseEvent event) {
			int x = event.x;
			int y = event.y;
			
			if(polygonIndex >= 0 && polygonIndex < polygons.size()) {
				List<Point> polygon = polygons.get(polygonIndex);
				if(pointIndex >= 0 && pointIndex < polygon.size()) {
					// Move point that is being dragged.
					polygon.set(pointIndex, new Point(x, y));
					
					tooltip = "(" + x + ", " + y + ")";
					tooltipX = event.x + 5;
					tooltipY = event.y - 5;
					
					firePolygonChanged();
				}
			}
		}
		
		public void mouseReleased(MouseEvent event) {
			polygonIndex = -1;
			tooltip = null;
		}
	}
	
	private List<List<Point>> polygons = new ArrayList<>();
	private String tooltip = null;
	private int tooltipX;
	private int tooltipY;
	private Tool currentTool = new AddPolygonTool();
	private Composite graphicsComposite;
	private Runnable changeCallback;
	
	public PolygonCanvas(Composite parent) {
		graphicsComposite = new Composite(parent, SWT.DOUBLE_BUFFERED);
		graphicsComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		graphicsComposite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				paint(event.gc);
			}
		});
		
		graphicsComposite.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent event) {
				if(event.count == 1) {
					currentTool.mouseReleased(event);
					graphicsComposite.redraw();
				}
			}
			
			public void mouseDown(MouseEvent event) {
				currentTool.mousePressed(event);
				graphicsComposite.redraw();
			}
			
			public void mouseDoubleClick(MouseEvent event) {
				currentTool.mouseDoubleClicked(event);
				graphicsComposite.redraw();
			}
		});
		
		graphicsComposite.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent event) {
				if((event.stateMask & SWT.BUTTON_MASK) == 0) {
					currentTool.mouseMoved(event);
				} else {
					currentTool.mouseDragged(event);
				}
				graphicsComposite.redraw();
			}
		});
	}
	
	public void setChangeCallback(Runnable callback) {
		this.changeCallback = callback;
	}
	
	private void firePolygonChanged() {
		if(changeCallback != null) {
			changeCallback.run();
		}
	}
	
	public void setPolygons(List<List<Point>> polygons) {
		this.polygons = copyPolygons(polygons);
		graphicsComposite.redraw();
	}
	
	public List<List<Point>> getPolygons() {
		return copyPolygons(this.polygons);
	}
	
	private static List<List<Point>> copyPolygons(List<List<Point>> polygons) {
		List<List<Point>> newPolygons = new ArrayList<>();
		
		for(List<Point> polygon:polygons) {
			List<Point> newPolygon = new ArrayList<>();
			for(Point point:polygon) {
				newPolygon.add(new Point(point));
			}
			newPolygons.add(newPolygon);
		}
		
		return newPolygons;
	}
	
	private static double distance(Point p1, Point p2) {
		double dx = p1.getX() - p2.getX();
		double dy = p1.getY() - p2.getY();
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	private double distanceToLine(Point point, Point point1, Point point2) {
		double a = point.getX() - point1.getX();
		double b = point.getY() - point1.getY();
		double c = point2.getX() - point1.getX();
		double d = point2.getY() - point1.getY();;

		double dot = a*c + b*d;
		double squareLength = c*c + d*d;
		
		if(squareLength < 1) {
			return distance(point, point1);
		}
		
		double p = dot / squareLength;

		if(p < 0) {
			return distance(point, point1);
		} else if(p > 1) {
			return distance(point, point2);
		} else {
			return distance(point, new Point(
				(int) (point1.getX() + p*c),
				(int) (point1.getY() + p*d))
			);
		}
	}
	
	private List<Point> getLastPolygon() {
		return polygons.get(polygons.size() - 1);
	}
	
	private void paint(GC gc) {
		for(List<Point> polygon:polygons) {
			gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
			drawPolygon(gc, polygon);
		}
		
		if(tooltip != null) {
			gc.drawString(tooltip, tooltipX, tooltipY - 10, true);
		}
	}
	
	private void drawPolygon(GC gc, List<Point> polygon) {
		for(int i = 0; i < polygon.size(); i++) {
			Point p1 = polygon.get(i);
			Point p2 = polygon.get((i + 1) % polygon.size());
			
			gc.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
			gc.fillRectangle((int) (p1.getX() - 2), (int) (p1.getY() - 2), 5, 5);
		}
	}

	public Control getControl() {
		return graphicsComposite;
	}

	public void selectAddPolygonTool() {
		currentTool = new AddPolygonTool();
	}

	public void selectRemovePolygonTool() {
		currentTool = new RemovePolygonTool();
	}

	public void selectModifyPointsTool() {
		currentTool = new ModifyPointsTool();
	}
}

/**
 * Edits a list of polygons, where each polygon is a list of points.
 */
public class PolygonTabbedEditor extends Editor implements TabbedView {
	private final EventBus eventBus;
	private final Composite control;
	private final PolygonCanvas canvas; 
	
	public PolygonTabbedEditor(Composite parent, EventBus eventBus, ScriptController scriptController) {
		super(eventBus, scriptController);
		
		this.control = new Composite(parent, SWT.NONE);
		this.eventBus = eventBus;
		
		GridLayout gridLayout = new GridLayout(1, false);
		control.setLayout(gridLayout);
		
		Control toolbar = createToolbar(control);
		this.canvas = new PolygonCanvas(control);
		
		GridData layout1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		GridData layout2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		
		canvas.getControl().setLayoutData(layout1);
		toolbar.setLayoutData(layout2);
		
		canvas.setChangeCallback(this::writeValue);
	}
	
	private Control createToolbar(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new FillLayout());
		createButton(panel, "Add Polygon", () -> canvas.selectAddPolygonTool());
		createButton(panel, "Remove Polygon", () -> canvas.selectRemovePolygonTool());
		createButton(panel, "Modify Points", () -> canvas.selectModifyPointsTool());
		return panel;
	}
	
	private void createButton(Composite parent, String label, Runnable action) {
		Button button = new Button(parent, SWT.NONE);
		button.setText(label);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				action.run();
			}
		});
	}

	public void readValue() {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				if(isPolygonList(value)) {
					Display.getDefault().asyncExec(() -> {
						if(!canvas.getControl().isDisposed()) {
							canvas.setPolygons((List<List<Point>>) value);
						}
					});
				}
			});
		}
	}
	
	public void writeValue() {
		if(reference != null) {
			reference.set(canvas.getPolygons()).thenRun(() ->
				eventBus.post(new MinorRefreshEvent(this))
			);
		}
	}

	private static boolean isPolygonList(Object list) {
		return isListOfOrEmpty(list, sublist -> isListOf(sublist, x -> x instanceof Point));
	}

	public Control getControl() {
		return control;
	}
}