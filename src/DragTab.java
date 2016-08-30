import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tracker;

public class DragTab {
	private final Shell shell;
	
	private Runnable dragCallback = null;
	private Set<CTabFolder> folders = new HashSet<>();

	public DragTab(Shell shell) {
		this.shell = shell;
		
		shell.setLayout(new FillLayout());
		
		SashForm verticalSashForm = new SashForm(shell, SWT.VERTICAL);
		
		CTabFolder folder1 = new CTabFolder(verticalSashForm, SWT.BORDER);
		
		SashForm horizontalSashForm = new SashForm(verticalSashForm, SWT.HORIZONTAL);
		CTabFolder folder2 = new CTabFolder(horizontalSashForm, SWT.BORDER);
		CTabFolder folder3 = new CTabFolder(horizontalSashForm, SWT.BORDER);
		
		folders.addAll(Arrays.asList(folder1, folder2, folder3));
		
		for(int x = 0; x < 9; x++) {
			CTabFolder folder = (x < 3) ? folder1 : folder2;
			if(x > 6) folder = folder3;
			
			Text text = new Text(folder, SWT.MULTI);
			text.setText("Text " + x);
			createTabItem(folder, text, "Item " + x);
		}
		
		setupTabFolder(folder1);
		setupTabFolder(folder2);
		setupTabFolder(folder3);
	}

	/**
	 * Adds a drag listener to a folder to allow repositioning of tab items.
	 */
	private void addDragDetectListener(CTabFolder folder) {
		folder.addDragDetectListener(new DragDetectListener() {
			public void dragDetected(DragDetectEvent event) {
				CTabItem dragItem = folder.getItem(new Point(event.x, event.y));
				if(dragItem != null) {
					// Create tracker to handle move events and display visual feedback.
					Tracker tracker = new Tracker(Display.getCurrent(), SWT.NONE);
					tracker.setStippled(false);
					tracker.addListener(SWT.Move, new Listener() {
						public void handleEvent(Event event) {
							dragCallback = null;
							
							for(CTabFolder folder:folders) {
								if(handleDrag(tracker, folder, dragItem)) {
									return;
								}
							}
						}
					});
					
					// Wait until drag is finished, then run callback if any is set.
					if(tracker.open()) {
						if(dragCallback != null) {
							dragCallback.run();
						}
					}
				}
			}
		});
	}

	/**
	 * Handles dragging within a folder to allow repositioning of tab items.
	 */
	private boolean handleDrag(Tracker tracker, CTabFolder folder, CTabItem dragItem) {
		int folderOffsetX = folder.toDisplay(0, 0).x;
		int folderOffsetY = folder.toDisplay(0, 0).y;
		
		Point point = folder.toControl(Display.getCurrent().getCursorLocation());
		tracker.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));

		// Check drag to tab headers.
		for(int i = 0; i < folder.getItemCount(); i++) {
			CTabItem item = folder.getItem(i);
			if(point.y >= 0 && point.y <= item.getBounds().height) {
				int width = item.getBounds().width;
				int startX = item.getBounds().x;
				int endX = item.getBounds().x + item.getBounds().width;
				
				if((i == 0 || folder.getItem(i - 1) != dragItem) && Math.abs(point.x - startX) < width / 2) {
					tracker.setStippled(false);
					tracker.setRectangles(new Rectangle[] {
						new Rectangle(folderOffsetX + startX, folderOffsetY + item.getBounds().y, 0, item.getBounds().height)
					});
					return true;
				}
				
				if(item != dragItem && Math.abs(point.x - endX) < width / 2) {
					tracker.setStippled(false);
					tracker.setRectangles(new Rectangle[] {
						new Rectangle(folderOffsetX + endX, folderOffsetY + item.getBounds().y, 0, item.getBounds().height)
					});
					return true;
				}
			}
		}
		
		// Check drag to folder client area.
		Rectangle clientArea = folder.getClientArea();
		if(clientArea.contains(point)) {
			int fromTop = Math.abs(point.y - clientArea.y);
			int fromBottom = Math.abs(point.y - (clientArea.y + clientArea.height));
			int fromLeft = Math.abs(point.x - clientArea.x);
			int fromRight = Math.abs(point.x - (clientArea.x + clientArea.width));
			
			int min = Integer.MAX_VALUE;
			min = Math.min(min, fromTop);
			min = Math.min(min, fromBottom);
			min = Math.min(min, fromLeft);
			min = Math.min(min, fromRight);
			
			int x = clientArea.x + folderOffsetX;
			int y = clientArea.y + folderOffsetY;
			int width = clientArea.width;
			int height = clientArea.height;
			
			if(fromTop == min) {
				tracker.setRectangles(new Rectangle[] {
					new Rectangle(x, y, width, height / 2 - 1),
					new Rectangle(x, y + height / 2 + 1, width, height / 2 - 1)
				});
				dragCallback = () -> split(folder, dragItem, 0, -1);
			} else if(fromBottom == min) {
				tracker.setRectangles(new Rectangle[] {
					new Rectangle(x, y, width, height / 2 - 1),
					new Rectangle(x, y + height / 2 + 1, width, height / 2 - 1)
				});
				dragCallback = () -> split(folder, dragItem, 0, 1);
			} else if(fromLeft == min) {
				tracker.setRectangles(new Rectangle[] {
					new Rectangle(x, y, width / 2 - 1, height),
					new Rectangle(x + width / 2 + 1, y, width / 2 - 1, height)
				});
				dragCallback = () -> split(folder, dragItem, -1, 0);
			} else if(fromRight == min) {
				tracker.setRectangles(new Rectangle[] {
					new Rectangle(x, y, width / 2 - 1, height),
					new Rectangle(x + width / 2 + 1, y, width / 2 - 1, height)
				});
				dragCallback = () -> split(folder, dragItem, 1, 0);
			}
			
			return true;
		}
		
		// No drag target found.
		tracker.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_NO));
		tracker.setRectangles(new Rectangle[0]);
		return false;
	}
	
	/**
	 * Splits a folder into two panes, putting draggedItem into one of them and the existing tabs the another.
	 * DraggedItem is put into the top pane if dy < 0, the left pane if dx < 0, bottom if dy > 0, or right if dx > 0.
	 */
	private void split(CTabFolder folder, CTabItem draggedItem, int dx, int dy) {
		CTabItem[] items = folder.getItems();
		
		Control draggedControl = draggedItem.getControl();
		
		List<Control> controls = new ArrayList<>();
		for(CTabItem item:items) {
			controls.add(item.getControl());
		}
		
		Composite parent = folder.getParent();
		
		boolean firstChild = parent.getChildren()[0] == folder;
		
		SashForm sashForm = new SashForm(parent, (dx == 0) ? SWT.VERTICAL : SWT.HORIZONTAL);
		
		if(firstChild) {
			sashForm.moveAbove(parent.getChildren()[0]);
		}
		
		CTabFolder folder1 = new CTabFolder(sashForm, SWT.BORDER);
		CTabFolder folder2 = new CTabFolder(sashForm, SWT.BORDER);
		
		for(int i = 0; i < controls.size(); i++) {
			Control control = controls.get(i);
			if(control != draggedControl) {
				createTabItem(folder1, control, items[i].getText());
			}
		}
		
		createTabItem(folder2, draggedControl, draggedItem.getText());
		
		folder.dispose();
		folders.remove(folder);
		
		draggedItem.dispose();
		
		if(dx < 0 || dy < 0) {
			folder2.moveAbove(folder1);
		}
		
		setupTabFolder(folder1);
		setupTabFolder(folder2);
		
		folders.add(folder1);
		folders.add(folder2);
		
		parent.layout();
	}
	
	/**
	 * Returns a new tab item for a folder containing the given control and text.
	 */
	private CTabItem createTabItem(CTabFolder folder, Control control, String text) {
		CTabItem item = new CTabItem(folder, SWT.NONE);
		control.setParent(folder);
		item.setText(text);
		item.setControl(control);
		setupTabItem(item);
		return item;
	}

	/**
	 * Sets default properties for a folder.
	 */
	private void setupTabFolder(CTabFolder folder) {
		folder.setSimple(false);
		folder.setTabHeight(24);
		
		if(folder.getItemCount() > 0) {
			folder.setSelection(folder.getItems()[0]);
		}
		
		addDragDetectListener(folder);
	}
	
	/**
	 * Sets default properties for a tab item.
	 */
	private void setupTabItem(CTabItem item) {
		item.setShowClose(true);
	}

	public static void main(String[] args) {
		Display display = new Display();
		
		Shell shell = new Shell(display);
		
		shell.setSize(900, 600);
		
		new DragTab(shell);
		
		shell.setVisible(true);
		
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		display.dispose();
	}
}

