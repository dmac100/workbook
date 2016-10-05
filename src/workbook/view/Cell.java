package workbook.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import workbook.layout.GridDataBuilder;
import workbook.script.ScriptController;
import workbook.script.ScriptFuture;
import workbook.view.result.Result;

public class Cell {
	private final Composite parent;
	private final Label prompt;
	private final Text command;
	
	private final List<Runnable> upCallbacks = new ArrayList<>();
	private final List<Runnable> downCallbacks = new ArrayList<>();
	private final List<Runnable> deleteCallbacks = new ArrayList<>();
	private final List<Runnable> runCallbacks = new ArrayList<>();
	private final List<Runnable> runAllCallbacks = new ArrayList<>();
	
	private final Result result;
	private Function<String, ScriptFuture<Object>> executeFunction = null;
	
	public Cell(Composite parent, ScriptController scriptController) {
		this.parent = parent;
		
		Display display = parent.getDisplay();
		
		prompt = addLabel(parent, ">>>");
		command = new Text(parent, SWT.NONE);
		result = new Result(parent, scriptController);
		
		command.setFont(FontList.consolas10);
		
		command.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent event) {
				event.text = event.text.replaceAll("(^[\r\n]+)|([\r\n]+$)", "");
			}
		});
		
		command.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent event) {
				evaluate(true);
			}
		});
		
		command.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				if(event.keyCode == SWT.CR && event.stateMask == SWT.CONTROL) {
					runAllCallbacks.forEach(Runnable::run);
					event.doit = false;
				}
			}
		});
		
		command.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if(event.character == SWT.BS && command.getText().isEmpty()) {
					deleteCallbacks.forEach(Runnable::run);
				} else if(event.keyCode == SWT.ARROW_UP) {
					upCallbacks.forEach(Runnable::run);
				} else if(event.keyCode == SWT.ARROW_DOWN) {
					downCallbacks.forEach(Runnable::run);
				}
			}
		});
		
		result.asComposite().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				if(event.keyCode == SWT.CR && event.stateMask == SWT.NONE) {
					evaluate(true);
				} else if(event.keyCode == SWT.CR && event.stateMask == SWT.CONTROL) {
					runAllCallbacks.forEach(Runnable::run);
					event.doit = false;
				}
			}
		});
		
		result.asComposite().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if(event.keyCode == SWT.ARROW_UP) {
					upCallbacks.forEach(Runnable::run);
				} else if(event.keyCode == SWT.ARROW_DOWN) {
					downCallbacks.forEach(Runnable::run);
				}
			}
		});
		
		command.setLayoutData(new GridDataBuilder().fillHorizontal().build());
		result.asComposite().setLayoutData(new GridDataBuilder().fillHorizontal().horizontalSpan(2).build());
		
		prompt.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		prompt.setForeground(display.getSystemColor(SWT.COLOR_DARK_CYAN));
	}
	
	public void evaluate(boolean fireCallbacks) {
		if(!command.getText().trim().isEmpty()) {
			result.setLoading();
			
			parent.pack();
			
			executeFunction.apply(command.getText()).thenAccept(resultObject -> {
				result.setValue(resultObject, () -> {
					Display.getDefault().asyncExec(() -> {
						if(fireCallbacks) {
							runCallbacks.forEach(Runnable::run);
						}
						parent.pack();
					});
				});
			});
		}
	}
	
	public String getCommand() {
		return command.getText();
	}
	
	public void setCommand(String text) {
		command.setText(text);
	}
	
	public void setExecuteFunction(Function<String, ScriptFuture<Object>> executeFunction) {
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
	
	public void addRunAllCallback(Runnable callback) {
		runAllCallbacks.add(callback);
	}
	
	public Rectangle getBounds() {
		Rectangle promptBounds = prompt.getBounds();
		Rectangle resultBounds = result.asComposite().getBounds();
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
		result.asComposite().dispose();
		prompt.dispose();
	}
	
	private static Label addLabel(Composite parent, String content) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(content);
		return label;
	}
}