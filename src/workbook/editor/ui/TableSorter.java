package workbook.editor.ui;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import workbook.util.NaturalOrderComparator;

/**
 * Allows sorting of a table by clicking on its column headers.
 */
public class TableSorter {
	private final Table table;
	
	private int sortColumn = -1;
	private boolean ascending = true;
	
	public TableSorter(Table table) {
		this.table = table;
	}
	
	/**
	 * Adds listeners to each column to enable sorting by column.
	 */
	public void addListeners() {
		for(TableColumn column:table.getColumns()) {
			column.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					int newSortColumn = Arrays.asList(table.getColumns()).indexOf(column);
					
					if(newSortColumn == sortColumn) {
						ascending = !ascending;
					} else {
						sortColumn = newSortColumn;
						ascending = true;
					}
					
					sortItems();
				}
			});
		}
	}
	
	/**
	 * Sorts the table based on sortColumn and ascending, recreating the tableItems in the new order.
	 */
	private void sortItems() {
		if(sortColumn >= 0 && sortColumn < table.getColumnCount()) {
			table.setSortColumn(table.getColumn(sortColumn));
			table.setSortDirection(ascending ? SWT.UP : SWT.DOWN);
			Comparator<String> natcmp = new NaturalOrderComparator();
			
			// Get table items in sorted order.
			TableItem[] items = table.getItems();
			Arrays.sort(items, (x, y) -> natcmp.compare(x.getText(sortColumn), y.getText(sortColumn)) * (ascending ? 1 : -1));
			
			for(int i = 0; i < items.length; i++) {
				// Create copy of item[i] at bottom of table.
				TableItem item = new TableItem(items[i].getParent(), items[i].getStyle());
				for(int j = 0; j < table.getColumnCount(); j++) {
					item.setText(j, items[i].getText(j));
				}
				item.setData(items[i].getData());
				
				// Dispose original item.
				items[i].dispose();
			}
		} else {
			table.setSortDirection(SWT.NONE);
		}
	}
}