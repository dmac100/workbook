import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
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
	private Runnable dragCallback = null;
	private Set<CTabFolder> folders = new HashSet<>();
	private int tabCount = 0;

	public DragTab(Composite parent) {
		parent.setLayout(new FillLayout());
		
		CTabFolder folder = new CTabFolder(parent, SWT.BORDER);
		folders.add(folder);
		setupTabFolder(folder);
		
		for(int x = 0; x < 8; x++) {
			Text text = new Text(parent, SWT.MULTI);
			newTab(folder, text);
		}
		
		if(folder.getItemCount() > 0) {
			folder.setSelection(0);
		}
	}
	
	public void newTab(CTabFolder folder, Control contents) {
		createTabItem(folder, contents, "Item " + tabCount++);
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
					dragCallback = () -> moveTabItem(dragItem, item, true);
					return true;
				}
				
				if(item != dragItem && Math.abs(point.x - endX) < width / 2) {
					tracker.setStippled(false);
					tracker.setRectangles(new Rectangle[] {
						new Rectangle(folderOffsetX + endX, folderOffsetY + item.getBounds().y, 0, item.getBounds().height)
					});
					dragCallback = () -> moveTabItem(dragItem, item, false);
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
	 * Moves the tab item, draggedItem, before or after destinationItem.
	 */
	private void moveTabItem(CTabItem draggedItem, CTabItem destinationItem, boolean placeBefore) {
		CTabFolder draggedFolder = draggedItem.getParent();
		
		int index = destinationItem.getParent().indexOf(destinationItem);
		if(!placeBefore) {
			index++;
		}
		
		// Check that tab is not already at destination.
		if(destinationItem.getParent().indexOf(draggedItem) != index) {
			CTabItem newTabItem = createTabItem(destinationItem.getParent(), draggedItem.getControl(), draggedItem.getText(), index);
			draggedItem.dispose();
			
			newTabItem.getParent().setSelection(newTabItem);
			
			removeIfEmpty(draggedFolder);
		}
	}

	/**
	 * Splits a folder into two panes, putting draggedItem into one of them and the existing tabs the another.
	 * DraggedItem is put into the top pane if dy < 0, the left pane if dx < 0, bottom if dy > 0, or right if dx > 0.
	 */
	private void split(CTabFolder folder, CTabItem draggedItem, int dx, int dy) {
		CTabFolder draggedFolder = draggedItem.getParent();
		Composite parent = folder.getParent();
		
		SashForm sashForm = new SashForm(parent, (dx == 0) ? SWT.VERTICAL : SWT.HORIZONTAL);
		sashForm.moveAbove(folder);
		
		folder.setParent(sashForm);
		
		Control control = draggedItem.getControl();
		String text = draggedItem.getText();
		
		draggedItem.dispose();
		CTabFolder newFolder = new CTabFolder(sashForm, SWT.BORDER);
		createTabItem(newFolder, control, text);
		if(dx < 0 || dy < 0) {
			newFolder.moveAbove(folder);
		}
		
		setupTabFolder(newFolder);
		folders.add(newFolder);
		
		parent.layout();
		
		removeIfEmpty(draggedFolder);
	}
	
	/**
	 * Removes folder if it contains no items.
	 */
	private void removeIfEmpty(CTabFolder folder) {
		if(!folder.isDisposed() && folder.getItemCount() == 0) {
			Composite parent = folder.getParent();
			if(parent instanceof SashForm) {
				merge((SashForm) parent, folder);
			}
		}
	}

	/**
	 * Merges the children in sashForm together, removing the empty folder.
	 */
	private void merge(SashForm sashForm, CTabFolder folder) {
		// Find the sashForm child that is not folder.
		Control nonEmptyChild = null;
		for(Control child:sashForm.getChildren()) {
			if(child instanceof CTabFolder || child instanceof SashForm) {
				if(child != folder) {
					nonEmptyChild = child;
				}
			}
		}
		
		folders.remove(folder);
		
		// Replace sashForm with nonEmptyChild.
		if(nonEmptyChild != null) {
			Composite parent = sashForm.getParent();
			nonEmptyChild.setParent(parent);
			nonEmptyChild.moveAbove(sashForm);
			sashForm.dispose();
			parent.layout();
		}
	}
	
	/**
	 * Returns a new tab item for a folder containing the given control and text.
	 */
	private CTabItem createTabItem(CTabFolder folder, Control control, String text) {
		return createTabItem(folder, control, text, folder.getItemCount());
	}

	/**
	 * Returns a new tab item for a folder containing the given control and text, at a specific index.
	 */
	private CTabItem createTabItem(CTabFolder folder, Control control, String text, int index) {
		CTabItem item = new CTabItem(folder, SWT.NONE, index);
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
		
		folder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close(CTabFolderEvent event) {
				Display.getCurrent().asyncExec(() -> removeIfEmpty(folder));
			}
		});
		
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
