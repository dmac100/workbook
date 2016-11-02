package workbook.editor.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.TreeAdapter;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.google.common.eventbus.EventBus;

import workbook.editor.ScriptTableUtil;
import workbook.editor.reference.Reference;
import workbook.event.MinorRefreshEvent;
import workbook.script.ScriptController;
import workbook.view.TabbedView;

/**
 * An editor that allows editing of properties within an object in a tree.
 */
public class TreeTabbedEditor extends Editor implements TabbedView {
	private final Composite parent;
	private final EventBus eventBus;
	private final ScriptTableUtil scriptTableUtil;
	
	private final Tree tree;
	private final TreeEditor treeEditor;
	private final List<List<String>> expandedItems = new ArrayList<>();
	
	public TreeTabbedEditor(Composite parent, EventBus eventBus, ScriptController scriptController) {
		this.parent = parent;
		this.eventBus = eventBus;
		this.scriptTableUtil = new ScriptTableUtil(scriptController);
		
		this.tree = new Tree(parent, SWT.NONE);
		this.treeEditor = new TreeEditor(tree);
		treeEditor.horizontalAlignment = SWT.LEFT;
		treeEditor.grabHorizontal = true;
		
		tree.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent event) {
				onMouseDown(event);
			}
		});
		
		tree.addTreeListener(new TreeAdapter() {
			public void treeExpanded(TreeEvent event) {
				expandItem((TreeItem) event.item);
			}
		});
	}
	
	private void onMouseDown(MouseEvent event) {
		checkBounds(tree.getItems(), event);
	}

	/**
	 * Checks whether the mouse event is within any items, and edits that item if so.
	 */
	private void checkBounds(TreeItem[] items, MouseEvent event) {
		Rectangle clientArea = tree.getClientArea();
		for(TreeItem item:items) {
			Rectangle bounds = item.getBounds(1);
			if(item.getExpanded()) {
				checkBounds(item.getItems(), event);
			} else {
				if(bounds.contains(new Point(event.x, event.y))) {
					editValue(item);
					return;
				}
			}
			if(!bounds.intersects(clientArea)) {
				return;
			}
		}
	}

	/**
	 * Activates an editor to edit the value of a tree item.
	 */
	private void editValue(TreeItem item) {
		Text text = new Text(tree, SWT.NONE);
		
		String originalValue = item.getText(1);
		Object itemData = item.getData();
		
		text.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent event) {
				if(!text.isDisposed()) {
					// Save value and dispose editor.
					if(!text.getText().equals(originalValue)) {
						writeItemValue(item, itemData, text.getText());
					}
					text.dispose();
				}
			}
		});
		
		text.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {
				if(!text.isDisposed()) {
					if(event.detail == SWT.TRAVERSE_RETURN) {
						// Save value and dispose editor.
						if(!text.getText().equals(originalValue)) {
							writeItemValue(item, itemData, text.getText());
						}
						text.dispose();
						event.doit = false;
					}
					
					if(event.detail == SWT.TRAVERSE_ESCAPE) {
						// Cancel editing and dispose editor.
						text.dispose();
						event.doit = false;
					}
				}
			}
		});
		
		treeEditor.setEditor(text, item, 1);
		text.setText(originalValue);
		text.selectAll();
		text.setFocus();
	}
	
	/**
	 * Refresh subitems when tree item is expanded.
	 */
	private void expandItem(TreeItem treeItem) {
		treeItem.setExpanded(true);
		Reference reference = (Reference) treeItem.getData();
		if(reference != null) {
			reference.get().thenAccept(value -> {
				if(value != null) {
					Map<String, Reference> rows = scriptTableUtil.getTableRow(value);
					tree.getDisplay().asyncExec(() -> {
						addTreeItems(treeItem, rows);
					});
				}
			});
		}
	}
	
	/**
	 * Sets the root tree items based on the current referenced value.
	 */
	public void readValue() {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				if(value != null) {
					Map<String, Reference> rows = scriptTableUtil.getTableRow(value);
					tree.getDisplay().asyncExec(() -> {
						if(!tree.isDisposed()) {
							setTreeItems(rows);
						}
					});
				}
			});
		}
	}
	
	/**
	 * Sets expandedItems to list of all tree items that are currently expanded, where each item is the full path of the item
	 * made up of the text content of the item and all parent items.
	 */
	private void getExpandedItems(List<List<String>> expandedItems, List<String> expandedPrefix, TreeItem[] treeItems) {
		for(TreeItem treeItem:treeItems) {
			if(treeItem.getExpanded()) {
				ArrayList<String> expandedItem = new ArrayList<>(expandedPrefix);
				expandedItem.add(treeItem.getText());
				expandedItems.add(expandedItem);
				
				getExpandedItems(expandedItems, expandedItem, treeItem.getItems());
			}
		}
	}
	
	/**
	 * Sets the root tree items to rows.
	 */
	private void setTreeItems(Map<String, Reference> rows) {
		expandedItems.clear();
		getExpandedItems(expandedItems, new ArrayList<>(), tree.getItems());
		
		for(TreeColumn treeColumn:tree.getColumns()) {
			treeColumn.dispose();
		}
		
		tree.setHeaderVisible(true);
		
		TreeColumn nameColumn = new TreeColumn(tree, SWT.NONE);
		nameColumn.setText("Name");
		nameColumn.setWidth(100);
		
		TreeColumn valueColumn = new TreeColumn(tree, SWT.NONE);
		valueColumn.setText("Value");
		valueColumn.setWidth(100);
		
		addTreeItems(tree, rows, expandedItems);
	}

	/**
	 * Adds tree items for rows to the tree.
	 */
	private void addTreeItems(Tree parent, Map<String, Reference> rows, List<List<String>> expandedItem) {
		TreeItem[] oldItems = parent.getItems();
		
		rows.forEach((name, value) -> {
			TreeItem treeItem = new TreeItem(parent, SWT.NONE);
			treeItem.setText(0, name);
			treeItem.setData(value);
			readItemValue(treeItem, value);
			
			treeItem.setExpanded(true);
		});
		
		for(TreeItem treeItem:oldItems) {
			treeItem.dispose();
		}
	}

	/**
	 * Adds tree items for rows to parent.
	 */
	private void addTreeItems(TreeItem parent, Map<String, Reference> rows) {
		TreeItem[] oldItems = parent.getItems();
		
		rows.forEach((name, value) -> {
			TreeItem treeItem = new TreeItem(parent, SWT.NONE);
			treeItem.setText(0, name);
			treeItem.setData(value);
			readItemValue(treeItem, value);
		});
		
		for(TreeItem treeItem:oldItems) {
			treeItem.dispose();
		}
	}
	
	/**
	 * Sets the treeItem value by reading the reference value.
	 */
	public void readItemValue(TreeItem treeItem, Reference reference) {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				boolean hasChild = !scriptTableUtil.getTableRow(value).isEmpty();
				String stringValue = hasChild ? "[" + String.valueOf(value.getClass()) + "]" : String.valueOf(value);
				treeItem.getDisplay().asyncExec(() -> {
					if(!treeItem.isDisposed()) {
						treeItem.setText(1, stringValue);
						if(hasChild) {
							new TreeItem(treeItem, SWT.NONE);
							if(shouldExpand(treeItem)) {
								expandItem(treeItem);
							}
						}
					}
				});
			});
		}
	}
	
	/**
	 * Returns whether a treeItem should be expanded based on whether it is represented in expandedItems.
	 */
	private boolean shouldExpand(TreeItem treeItem) {
		List<String> path = new ArrayList<>();
		TreeItem parent = treeItem.getParentItem();
		path.add(treeItem.getText());
		while(parent != null) {
			path.add(parent.getText());
			parent = parent.getParentItem();
		}
		Collections.reverse(path);
		return expandedItems.contains(path);
	}

	/**
	 * Writes value to the reference of treeItem.
	 */
	private void writeItemValue(TreeItem treeItem, Object itemData, String value) {
		if(!treeItem.isDisposed()) {
			treeItem.setText(1, "");
		}
		
		Reference reference = (Reference) itemData;
		if(reference != null) {
			reference.set(value).thenRunAlways(() -> {
				eventBus.post(new MinorRefreshEvent(this));
				readItemValue(treeItem, reference);
			});
		}
	}
	
	public Control getControl() {
		return tree;
	}
}
