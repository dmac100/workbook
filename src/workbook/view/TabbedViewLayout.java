package workbook.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tracker;
import org.jdom2.Element;

/**
 * A tabbed view layout that displays views arranged in tabs and allows the dragged of tabs between views
 * to split and merge tab folders together.
 */
public class TabbedViewLayout {
	public enum FolderPosition {
		LEFT, RIGHT, BOTTOM
	}
	
	private final Composite parent;
	
	private Runnable dragCallback = null;
	private Set<CTabFolder> folders = new HashSet<>();
	
	private CTabFolder leftFolder;
	private CTabFolder rightFolder;
	private CTabFolder bottomFolder;

	public TabbedViewLayout(Composite parent) {
		this.parent = parent;
		parent.setLayout(new FillLayout());
		
		leftFolder = new CTabFolder(parent, SWT.BORDER);
		folders.add(leftFolder);
		setupTabFolder(leftFolder);
		
		if(leftFolder.getItemCount() > 0) {
			leftFolder.setSelection(0);
		}
		
		bottomFolder = split(leftFolder, 0, 1, 60);
		rightFolder = split(leftFolder, 1, 0, 60);
	}
	
	public void removeEmptyFolders() {
		new ArrayList<>(folders).forEach(this::removeIfEmpty);
	}
	
	public void clear() {
		for(Control child:parent.getChildren()) {
			child.dispose();
		}
		
		folders.clear();
		leftFolder = null;
		rightFolder = null;
		bottomFolder = null;
	}
	
	public <T extends TabbedView> T addTab(CTabFolder folder, String title, Function<Composite, T> viewFactory) {
		if(folder == null || folder.isDisposed()) {
			folder = folders.iterator().next();
		}
		T view = viewFactory.apply(folder);
		CTabItem tabItem = createTabItem(folder, view.getControl(), title, view);
		folder.setSelection(folder.getItemCount() - 1);
		return view;
	}
	
	public CTabFolder getFolder(FolderPosition position) {
		switch(position) {
			case LEFT: return leftFolder;
			case RIGHT: return rightFolder;
			case BOTTOM: return bottomFolder;
		}
		throw new IllegalArgumentException("Unknown position: " + position);
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
					dragCallback = null;
					Tracker tracker = new Tracker(Display.getCurrent(), SWT.NONE);
					tracker.setStippled(false);
					tracker.addListener(SWT.Move, new Listener() {
						public void handleEvent(Event event) {
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
							restoreFolder(folder);
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
		
		Rectangle clientArea = folder.getClientArea();

		// Check if point is within folder bounds.
		if(!(point.x >= 0 && point.y >= 0 && point.x < folder.getBounds().width && point.y < folder.getBounds().height)) {
			tracker.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_NO));
			tracker.setRectangles(new Rectangle[0]);
			return false;
		}
		
		// Check drag to empty folder.
		if(folder.getItemCount() == 0) {
			int x = clientArea.x + folderOffsetX;
			int y = clientArea.y + folderOffsetY;
			int width = clientArea.width;
			int height = clientArea.height;
			
			tracker.setStippled(false);
			tracker.setRectangles(new Rectangle[] {
				new Rectangle(x, y, width, height)
			});
			dragCallback = () -> moveToEmptyFolder(folder, dragItem);
			return true;
		}

		// Check drag to tab headers.
		for(int i = 0; i < folder.getItemCount(); i++) {
			CTabItem item = folder.getItem(i);
			if(point.y >= 0 && point.y <= item.getBounds().height) {
				int width = item.getBounds().width;
				int startX = item.getBounds().x;
				int endX = item.getBounds().x + item.getBounds().width;
				
				// Check placement to the left of item i.
				if((i == 0 || folder.getItem(i - 1) != dragItem) && Math.abs(point.x - startX) < width / 2) {
					tracker.setStippled(false);
					tracker.setRectangles(new Rectangle[] {
						new Rectangle(folderOffsetX + startX, folderOffsetY + item.getBounds().y, 0, item.getBounds().height)
					});
					dragCallback = () -> moveTabItem(dragItem, item, true);
					return true;
				}
				
				// Check placement to the right of item i.
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
		
		// Check drag to right all tab items.
		if(folder.getItemCount() > 0) {
			CTabItem item = folder.getItem(folder.getItemCount() - 1);
			if(point.y >= 0 && point.y <= item.getBounds().height) {
				int endX = item.getBounds().x + item.getBounds().width;
				if(item != dragItem && point.x > endX) {
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
				dragCallback = () -> split(folder, dragItem, 0, -1, 50);
			} else if(fromBottom == min) {
				tracker.setRectangles(new Rectangle[] {
					new Rectangle(x, y, width, height / 2 - 1),
					new Rectangle(x, y + height / 2 + 1, width, height / 2 - 1)
				});
				dragCallback = () -> split(folder, dragItem, 0, 1, 50);
			} else if(fromLeft == min) {
				tracker.setRectangles(new Rectangle[] {
					new Rectangle(x, y, width / 2 - 1, height),
					new Rectangle(x + width / 2 + 1, y, width / 2 - 1, height)
				});
				dragCallback = () -> split(folder, dragItem, -1, 0, 50);
			} else if(fromRight == min) {
				tracker.setRectangles(new Rectangle[] {
					new Rectangle(x, y, width / 2 - 1, height),
					new Rectangle(x + width / 2 + 1, y, width / 2 - 1, height)
				});
				dragCallback = () -> split(folder, dragItem, 1, 0, 50);
			}
			
			return true;
		}
		
		// No drag target found.
		tracker.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_NO));
		tracker.setRectangles(new Rectangle[0]);
		return false;
	}

	/**
	 * Moves the tab item, draggedItem, to an empty folder.
	 */
	private void moveToEmptyFolder(CTabFolder folder, CTabItem draggedItem) {
		CTabFolder draggedFolder = draggedItem.getParent();
		
		CTabItem newTabItem = createTabItem(folder, draggedItem.getControl(), draggedItem.getText(), (TabbedView) draggedItem.getData());
		draggedItem.setControl(null);
		draggedItem.dispose();
		
		newTabItem.getParent().setSelection(newTabItem);
		
		removeIfEmpty(draggedFolder);
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
			CTabItem newTabItem = createTabItem(destinationItem.getParent(), draggedItem.getControl(), draggedItem.getText(), (TabbedView) draggedItem.getData(), index);
			draggedItem.setControl(null);
			draggedItem.dispose();
			
			newTabItem.getParent().setSelection(newTabItem);
			
			removeIfEmpty(draggedFolder);
		}
	}

	/**
	 * Splits a folder into two panes, putting draggedItem into one of them, and the existing tabs in the other.
	 * DraggedItem is put into the top pane if dy < 0, the left pane if dx < 0, bottom if dy > 0, or right if dx > 0.
	 * Weight is the percentage that the left or top folder takes up.
	 */
	private void split(CTabFolder folder, CTabItem draggedItem, int dx, int dy, int weight) {
		CTabFolder draggedFolder = draggedItem.getParent();
		Control control = draggedItem.getControl();
		TabbedView data = (TabbedView) draggedItem.getData();
		String text = draggedItem.getText();
		
		draggedItem.setControl(null);
		draggedItem.dispose();
		
		CTabFolder newFolder = split(folder, dx, dy, weight);
		createTabItem(newFolder, control, text, data);
		
		
		removeIfEmpty(draggedFolder);
		
		newFolder.setSelection(0);
	}
	
	/**
	 * Splits a folder into two panes, putting a new folder in one of them, and the existing tabs in the other.
	 * The new folder is put into the top pane if dy < 0, the left pane if dx < 0, bottom if dy > 0, or right if dx > 0.
	 * Weight is the percentage that the left or top folder takes up.
	 */
	private CTabFolder split(CTabFolder folder, int dx, int dy, int weight) {
		Composite parent = folder.getParent();
		parent.layout();
		
		int[] oldWeights = null;
		if(parent instanceof SashForm) {
			oldWeights = ((SashForm) parent).getWeights();
		}
		
		SashForm sashForm = new SashForm(parent, (dx == 0) ? SWT.VERTICAL : SWT.HORIZONTAL);
		sashForm.moveAbove(folder);
		
		folder.setParent(sashForm);
		
		CTabFolder newFolder = new CTabFolder(sashForm, SWT.BORDER);
		if(dx < 0 || dy < 0) {
			newFolder.moveAbove(folder);
		}
		
		setupTabFolder(newFolder);
		folders.add(newFolder);
		
		sashForm.setWeights(new int[] { weight, 100 - weight });
		parent.layout();
		
		if(parent instanceof SashForm) {
			((SashForm) parent).setWeights(oldWeights);
		}
		
		return newFolder;
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
			if(!(parent instanceof SashForm)) {
				nonEmptyChild.setLayoutData(null);
			}
			nonEmptyChild.setParent(parent);
			nonEmptyChild.moveAbove(sashForm);
			sashForm.dispose();
			parent.layout();
		}
	}
	
	/**
	 * Returns a new tab item for a folder containing the given control and text.
	 */
	private CTabItem createTabItem(CTabFolder folder, Control control, String text, TabbedView data) {
		return createTabItem(folder, control, text, data, folder.getItemCount());
	}

	/**
	 * Returns a new tab item for a folder containing the given control and text, at a specific index.
	 */
	private CTabItem createTabItem(CTabFolder folder, Control control, String text, TabbedView data, int index) {
		CTabItem item = new CTabItem(folder, SWT.NONE, index);
		control.setParent(folder);
		item.setText(text);
		item.setControl(control);
		item.setData(data);
		setupTabItem(item);
		return item;
	}

	/**
	 * Sets default properties for a folder.
	 */
	private void setupTabFolder(CTabFolder folder) {
		folder.setSimple(false);
		folder.setMenu(createContextMenu(folder));
		folder.setMaximizeVisible(true);
		folder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void restore(CTabFolderEvent event) {
				restoreFolder(folder);
			}
			
			public void maximize(CTabFolderEvent event) {
				maximizeFolder(folder);
			}
		});
		
		if(folder.getItemCount() > 0) {
			folder.setSelection(0);
		}
		
		folder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close(CTabFolderEvent event) {
				Display.getCurrent().asyncExec(() -> removeIfEmpty(folder));
			}
		});
		
		addDragDetectListener(folder);
	}
	
	private void maximizeFolder(CTabFolder folder) {
		folder.setMaximized(true);
		for(Control control = folder; control != parent; control = control.getParent()) {
			if(control.getParent() instanceof SashForm) {
				((SashForm) control.getParent()).setMaximizedControl(control);
			}
		}
	}
	
	private void restoreFolder(CTabFolder folder) {
		folder.setMaximized(false);
		for(Control control = folder; control != parent; control = control.getParent()) {
			if(control instanceof SashForm) {
				((SashForm) control).setMaximizedControl(null);
			}
		}
	}
	
	private void maximize(Control control, CTabFolder folder) {
		if(control instanceof SashForm) {
			maximize((SashForm) control, folder);
		}
	}
	
	private Menu createContextMenu(CTabFolder folder) {
		Menu menu = new Menu(folder);
		
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent event) {
				// Dispose old items.
				for(MenuItem menuItems:menu.getItems()) {
					menuItems.dispose();
				}
				
				// Create default items.
				MenuItem renameItem = new MenuItem(menu, SWT.NONE);
				renameItem.setText("Rename...");
				renameItem.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent event) {
						CTabItem tabItem = folder.getSelection();
						if(tabItem != null) {
							String name = InputDialog.open(folder.getShell(), "Name", "Name");
							if(name != null) {
								tabItem.setText(name);
							}
						}
					}
				});
				
				// Create menu for specific item.
				CTabItem tabItem = folder.getSelection();
				if(tabItem != null) {
					TabbedView tabbedView = ((TabbedView) tabItem.getData());
					tabbedView.createMenu(menu);
				}
			}
		});
		
		return menu;
	}

	/**
	 * Sets default properties for a tab item.
	 */
	private void setupTabItem(CTabItem item) {
		item.setShowClose(true);
		item.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				if(item.getControl() != null) {
					item.getControl().dispose();
				}
			}
		});
	}
	
	/**
	 * Serializes the state of the view and adds it to element.
	 */
	public void serialize(Element element) {
		for(Control control:parent.getChildren()) {
			serialize(element, control);
		}
	}
	
	private void serialize(Element parent, Control control) {
		if(control instanceof CTabFolder) {
			serialize(parent, (CTabFolder) control);
		} else if(control instanceof SashForm) {
			serialize(parent, (SashForm) control);
		}
	}

	private void serialize(Element parent, SashForm sashForm) {
		int[] weights = sashForm.getWeights();
		
		Element splitElement = new Element("Split");
		splitElement.setAttribute("weight1", String.valueOf(weights[0]));
		splitElement.setAttribute("weight2", String.valueOf(weights[1]));
		splitElement.setAttribute("orientation", (sashForm.getOrientation() == SWT.HORIZONTAL) ? "horizontal" : "vertical");
		parent.addContent(splitElement);
		
		for(Control control:sashForm.getChildren()) {
			serialize(splitElement, control);
		}
	}

	private void serialize(Element parent, CTabFolder tabFolder) {
		Element folderElement = new Element("Items");
		parent.addContent(folderElement);
		
		for(CTabItem tabItem:tabFolder.getItems()) {
			TabbedView tabbedView = (TabbedView) tabItem.getData();
			
			Element itemElement = new Element("Item");
			itemElement.setAttribute("title", tabItem.getText());
			itemElement.setAttribute("type", tabbedView.getClass().getSimpleName());
			folderElement.addContent(itemElement);
			
			tabbedView.serialize(itemElement);
		}
	}

	/**
	 * Deserializes the state of the view from the given element.
	 */
	public void deserialize(TabbedViewFactory viewFactory, Element parentElement) {
		clear();
		
		CTabFolder folder = new CTabFolder(parent, SWT.BORDER);
		folders.add(folder);
		setupTabFolder(folder);
		
		deserialize(viewFactory, parentElement.getChildren().get(0), folder);
		
		parent.layout();
	}

	private void deserialize(TabbedViewFactory viewFactory, Element element, CTabFolder folder) {
		if(element.getName().equals("Split")) {
			int weight1 = Integer.parseInt(element.getAttributeValue("weight1"));
			int weight2 = Integer.parseInt(element.getAttributeValue("weight2"));
			String orientation = element.getAttributeValue("orientation");
			int weight = (weight1 * 100) / (weight1 + weight2);
			
			CTabFolder newFolder;
			if(orientation.equals("horizontal")) {
				newFolder = split(folder, 1, 0, weight);
			} else {
				newFolder = split(folder, 0, 1, weight);
			}
			
			if(element.getChildren().size() == 2) {
				deserialize(viewFactory, element.getChildren().get(0), folder);
				deserialize(viewFactory, element.getChildren().get(1), newFolder);
			}
		} else if(element.getName().equals("Items")) {
			for(Element child:element.getChildren()) {
				if(child.getName().equals("Item")) {
					String title = child.getAttributeValue("title");
					String type = child.getAttributeValue("type");
					TabbedView view = viewFactory.addView(type, folder, title);
					view.deserialize(child);
				}
			}
		}
	}
}