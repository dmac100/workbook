package workbook.view.result;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import workbook.editor.ScriptTableUtil;
import workbook.editor.reference.Reference;
import workbook.script.Engine;
import workbook.script.ScriptController;

public class TableRenderer implements ResultRenderer {
	private final ResultRenderer next;
	private final ScriptController scriptController;

	public TableRenderer(ResultRenderer next, ScriptController scriptController) {
		this.next = next;
		this.scriptController = scriptController;
	}
	
	public void addView(Composite parent, Object value, Runnable callback) {
		scriptController.exec(() -> {
			ScriptTableUtil scriptTableUtil = new ScriptTableUtil(scriptController);
			
			Engine script = scriptController.getScriptSync();
			
			if(value != null) {
				Map<String, List<Reference>> columns = scriptTableUtil.getTable(value);
				
				if(scriptTableUtil.isIterable(value)) {
					if(!columns.isEmpty()) {
						addTable(parent, value, callback);
						return null;
					}
				}
			}
			
			next.addView(parent, value, callback);
			return null;
		});
	}

	private void addTable(Composite parent, Object value, Runnable callback) {
		Map<String, List<Reference>> columns = new ScriptTableUtil(scriptController).getTable(value);
		
		Display.getDefault().asyncExec(() -> {
			Table table = new Table(parent, SWT.BORDER);
			table.setHeaderVisible(true);
			
			columns.forEach((name, values) -> {
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
				tableItem.setData(values);
				for(int i = 0; i < values.size(); i++) {
					int index = i;
					Reference reference = values.get(index);
					readItemValue(tableItem, index + 1, reference);
				}
			});
			
			callback.run();
		});
	}
	
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
}
