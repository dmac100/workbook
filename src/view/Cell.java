package view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import layout.GridDataBuilder;

public class Cell {
	private final Label prompt;
	private final Text command;
	private final Text result;
	
	private final List<Runnable> upCallbacks = new ArrayList<>();
	private final List<Runnable> downCallbacks = new ArrayList<>();
	private final List<Runnable> deleteCallbacks = new ArrayList<>();
	private final List<Runnable> runCallbacks = new ArrayList<>();
	
	private Function<String, String> executeFunction = null;
	
	public Cell(Composite parent) {
		Display display = parent.getDisplay();
		
		prompt = addLabel(parent, ">>>");
		command = new Text(parent, SWT.NONE);
		result = new Text(parent, SWT.NONE);
		
		command.setFont(FontList.consolas10);
		result.setFont(FontList.consolas10);
		
		command.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent event) {
				String resultText = executeFunction.apply(command.getText());
				result.setText(String.valueOf(resultText));
				
				for(Runnable callback:runCallbacks) {
					callback.run();
				}
			}
		});
		
		command.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if(event.character == SWT.BS && command.getText().isEmpty()) {
					for(Runnable callback:deleteCallbacks) {
						callback.run();
					}
				} else if(event.keyCode == SWT.ARROW_UP) {
					for(Runnable callback:upCallbacks) {
						callback.run();
					}
				} else if(event.keyCode == SWT.ARROW_DOWN) {
					for(Runnable callback:downCallbacks) {
						callback.run();
					}
				}
			}
		});
		
		command.setLayoutData(new GridDataBuilder().fillHorizontal().build());
		result.setLayoutData(new GridDataBuilder().fillHorizontal().horizontalSpan(2).build());
		
		result.setEditable(false);
		
		prompt.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		command.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		result.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		
		prompt.setForeground(display.getSystemColor(SWT.COLOR_DARK_CYAN));
	}
	
	public String getCommand() {
		return command.getText();
	}
	
	public String getResult() {
		return result.getText();
	}
	
	public void setExecuteFunction(Function<String, String> executeFunction) {
		this.executeFunction = executeFunction;
	}
	
	public void addUpCallback(Runnable callback) {
		upCallbacks.add(callback);
	}
	
	public void addDownCallback(Runnable callback) {
		downCallbacks.add(callback);
	}
	
	public void addDeleteCallback(Runnable callback) {
		deleteCallbacks.add(callback);
	}
	
	public void addRunCallback(Runnable callback) {
		runCallbacks.add(callback);
	}
	
	public Rectangle getBounds() {
		Rectangle promptBounds = prompt.getBounds();
		Rectangle resultBounds = result.getBounds();
		Rectangle commandBounds = prompt.getBounds();
		
		int x = min(promptBounds.x, resultBounds.x, commandBounds.x);
		int y = min(promptBounds.y, resultBounds.y, commandBounds.y);
		int width = max(
			promptBounds.x - x + promptBounds.width,
			resultBounds.x - x + resultBounds.width,
			commandBounds.x - x + commandBounds.width
		);
		int height = max(
			promptBounds.y - y + promptBounds.height,
			resultBounds.y - y + resultBounds.height,
			commandBounds.y - y + commandBounds.height
		);
		
		return new Rectangle(x, y, width, height);
	}
	
	private static int min(int... values) {
		int min = Integer.MAX_VALUE;
		for(int value:values) {
			min = Math.min(min, value);
		}
		return min;
	}
	
	private static int max(int... values) {
		int max = Integer.MIN_VALUE;
		for(int value:values) {
			max = Math.max(max, value);
		}
		return max;
	}
	
	public void setFocus() {
		command.setSelection(command.getText().length());
		command.setFocus();
	}
	
	public void dispose() {
		command.dispose();
		result.dispose();
		prompt.dispose();
	}
	
	private static Label addLabel(Composite parent, String content) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(content);
		return label;
	}
}