package workbook.editor.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.google.common.eventbus.EventBus;

import workbook.editor.ScriptTableUtil;
import workbook.editor.reference.Reference;
import workbook.event.MinorRefreshEvent;
import workbook.script.ScriptController;
import workbook.view.TabbedView;

/**
 * An editor that allows editing of properties within an object in a table.
 */
public class TableTabbedEditor extends Editor implements TabbedView {
	private final Composite parent;
	private final EventBus eventBus;
	private final ScriptTableUtil scriptTableUtil;
	
	private final Table table;
	private final TableEditor tableEditor;

	public TableTabbedEditor(Composite parent, EventBus eventBus, ScriptController scriptController) {
		this.parent = parent;
		this.eventBus = eventBus;
		this.scriptTableUtil = new ScriptTableUtil(scriptController);
		
		this.table = new Table(parent, SWT.NONE);
		this.tableEditor = new TableEditor(table);
		tableEditor.horizontalAlignment = SWT.LEFT;
		tableEditor.grabHorizontal = true;
		
		table.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent event) {
				onMouseDown(event);
			}
		});
	}

	/**
	 * Creates an editor for an item if the mouse event is within that item.
	 */
	private void onMouseDown(MouseEvent event) {
		Rectangle clientArea = table.getClientArea();
		for(int y = table.getTopIndex(); y < table.getItemCount(); y++) {
			boolean visible = false;
			TableItem item = table.getItem(y);
			for(int x = 0; x < table.getColumnCount(); x++) {
				Rectangle bounds = item.getBounds(x);
				if(bounds.contains(new Point(event.x, event.y))) {
					editValue(item, x);
					return;
				}
				if(bounds.intersects(clientArea)) {
					visible = true;
				}
			}
			if(!visible) {
				return;
			}
		}
	}

	/**
	 * Creates an editor for an item, at a column.
	 */
	private void editValue(TableItem item, int column) {
		Text text = new Text(table, SWT.NONE);
		
		String originalValue = item.getText(column);
		Object itemData = item.getData();
		
		text.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent event) {
				if(!text.isDisposed()) {
					// Save value and dispose editor.
					if(!text.getText().equals(originalValue)) {
						writeItemValue(item, itemData, column, text.getText());
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
							writeItemValue(item, itemData, column, text.getText());
						}
						text.dispose();
						event.doit = false;
					}
					
					if(event.detail == SWT.TRAVERSE_ESCAPE) {
						// Cancel and dispose editor.
						text.dispose();
						event.doit = false;
					}
				}
			}
		});
		
		tableEditor.setEditor(text, item, column);
		text.setText(originalValue);
		text.selectAll();
		text.setFocus();
	}
	
	/**
	 * Sets the table items based on the current reference value.
	 */
	public void readValue() {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				if(value != null) {
					Map<String, List<Reference>> rows = scriptTableUtil.getTable(value);
					table.getDisplay().asyncExec(() -> {
						if(!table.isDisposed()) {
							setTableData(rows);
						}
					});
				}
			});
		}
	}
	
	/**
	 * Adds the table items based on columns.
	 */
	private void setTableData(Map<String, List<Reference>> columns) {
		for(TableItem tableItem:table.getItems()) {
			tableItem.dispose();
		}
		
		for(TableColumn tableColummn:table.getColumns()) {
			tableColummn.dispose();
		}
		
		table.setHeaderVisible(true);
		
		List<TableItem> rows = new ArrayList<>();
		
		Map<String, Integer> columnIndexes = new HashMap<>();
		
		// Add columns.
		columns.forEach((name, values) -> {
			TableColumn nameColumn = new TableColumn(table, SWT.NONE);
			nameColumn.setText(name);
			nameColumn.setWidth(100);
			
			columnIndexes.put(name, columnIndexes.size());
		});
		
		// Add table items with values.
		columns.forEach((name, values) -> {
			for(int i = 0; i < values.size(); i++) {
				if(rows.size() <= i) {
					rows.add(new TableItem(table, SWT.NONE));
				}
				
				TableItem item = rows.get(i);
				
				Reference reference = values.get(i);
				readItemValue(item, columnIndexes.get(name), reference);
			}

		});
		
		// Set references data for each row.
		for(int i = 0; i < rows.size(); i++) {
			List<Reference> references = new ArrayList<>();
			for(List<Reference> values:columns.values()) {
				references.add(values.get(i));
			}
			rows.get(i).setData(references);
		}
	}
	
	/**
	 * Reads the table item value from the reference.
	 */
	public void readItemValue(TableItem tableItem, int index, Reference reference) {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				String stringValue = String.valueOf(value);
				tableItem.getDisplay().asyncExec(() -> {
					if(!tableItem.isDisposed()) {
						tableItem.setText(index, stringValue);
					}
				});
			});
		}
	}

	/**
	 * Writes vaue to the reference of tableItem.
	 */
	private void writeItemValue(TableItem tableItem, Object itemData, int index, String value) {
		if(!tableItem.isDisposed()) {
			tableItem.setText(index, "");
		}
		
		List<Reference> references = (List<Reference>) itemData;
		if(references != null && index < references.size()) {
			Reference reference = references.get(index);
			if(reference != null) {
				reference.set(value).thenRunAlways(() -> {
					eventBus.post(new MinorRefreshEvent());
					readItemValue(tableItem, index, reference);
				});
			}
		}
	}
	
	public Control getControl() {
		return table;
	}
}