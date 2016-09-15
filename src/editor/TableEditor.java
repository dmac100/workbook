package editor;

import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class TableEditor implements Editor {
	private final Composite parent;
	private final String expression;
	private final ScriptTableUtil scriptTableUtil;
	
	private final Table table;
	
	private Reference reference;
	
	public TableEditor(Composite parent, String expression, ScriptTableUtil scriptTableUtil) {
		this.parent = parent;
		this.expression = expression;
		this.scriptTableUtil = scriptTableUtil;
		
		this.table = new Table(parent, SWT.NONE);
	}

	@Override
	public void setReference(Reference reference) {
		this.reference = reference;
		readValue();
	}

	public String getExpression() {
		return expression;
	}
	
	public void readValue() {
		if(reference != null) {
			reference.get().thenAccept(value -> {
				if(value != null) {
					try {
						Map<String, List<Reference>> rows = scriptTableUtil.getTable(value);
						table.getDisplay().asyncExec(() -> {
							if(!table.isDisposed()) {
								setTableRows(rows);
							}
						});
					} catch (ScriptException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
	
	private void setTableRows(Map<String, List<Reference>> rows) {
		for(TableItem tableItem:table.getItems()) {
			tableItem.dispose();
		}
		
		for(TableColumn tableColummn:table.getColumns()) {
			tableColummn.dispose();
		}
		
		table.setHeaderVisible(true);
		
		rows.forEach((name, values) -> {
			if(table.getItemCount() == 0) {
				TableColumn nameColumn = new TableColumn(table, SWT.NONE);
				nameColumn.setText("Name");
				nameColumn.setWidth(100);
				
				for(int i = 0; i < values.size(); i++) {
					TableColumn valueColumn = new TableColumn(table, SWT.NONE);
					valueColumn.setText("Value");
					valueColumn.setWidth(100);
				}
			}
			
			TableItem tableItem = new TableItem(table, SWT.NONE);
			tableItem.setText(0, name);
			for(int i = 0; i < values.size(); i++) {
				int index = i;
				Reference reference = values.get(index);
				if(reference != null) {
					reference.get().thenAccept(value -> {
						String stringValue = String.valueOf(value);
						tableItem.getDisplay().asyncExec(() -> {
							if(!tableItem.isDisposed()) {
								tableItem.setText(index + 1, stringValue);
							}
						});
					});
				}
			}
		});
	}

	public void writeValue() {
	}
	
	public Control getControl() {
		return table;
	}
}
