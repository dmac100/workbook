package editor.ui;

import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.swt.SWT;
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

import editor.ScriptTableUtil;
import editor.reference.Reference;

public class TableEditor implements Editor {
	private final Composite parent;
	private final String expression;
	private final ScriptTableUtil scriptTableUtil;
	
	private final Table table;
	private final org.eclipse.swt.custom.TableEditor tableEditor;
	
	private Reference reference;
	
	public TableEditor(Composite parent, String expression, ScriptTableUtil scriptTableUtil) {
		this.parent = parent;
		this.expression = expression;
		this.scriptTableUtil = scriptTableUtil;
		
		this.table = new Table(parent, SWT.NONE);
		this.tableEditor = new org.eclipse.swt.custom.TableEditor(table);
		tableEditor.horizontalAlignment = SWT.LEFT;
		tableEditor.grabHorizontal = true;
		
		table.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent event) {
				onMouseDown(event);
			}
		});
	}
	
	private void onMouseDown(MouseEvent event) {
		Rectangle clientArea = table.getClientArea();
		for(int y = table.getTopIndex(); y < table.getItemCount(); y++) {
			boolean visible = false;
			TableItem item = table.getItem(y);
			for(int x = 1; x < table.getColumnCount(); x++) {
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

	private void editValue(TableItem item, int x) {
		Text text = new Text(table, SWT.NONE);
		
		text.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent event) {
				if(!text.isDisposed()) {
					writeItemValue(item, x, text.getText());
					text.dispose();
				}
			}
		});
		
		text.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {
				if(!text.isDisposed()) {
					if(event.detail == SWT.TRAVERSE_RETURN) {
						writeItemValue(item, x, text.getText());
						text.dispose();
						event.doit = false;
					}
					
					if(event.detail == SWT.TRAVERSE_ESCAPE) {
						text.dispose();
						event.doit = false;
					}
				}
			}
		});
		
		tableEditor.setEditor(text, item, x);
		text.setText(item.getText(x));
		text.selectAll();
		text.setFocus();
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
			tableItem.setData(values);
			for(int i = 0; i < values.size(); i++) {
				int index = i;
				Reference reference = values.get(index);
				readItemValue(tableItem, index + 1, reference);
			}
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
	
	private void writeItemValue(TableItem tableItem, int index, String value) {
		tableItem.setText(index, "");
		
		List<Reference> references = (List<Reference>) tableItem.getData();
		if(references != null && index - 1 < references.size()) {
			Reference reference = references.get(index - 1);
			if(reference != null) {
				reference.set(value);
				readItemValue(tableItem, index, reference);
			}
		}
	}
	
	public Control getControl() {
		return table;
	}
}
