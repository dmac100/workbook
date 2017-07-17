package workbook.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import workbook.layout.GridDataBuilder;
import workbook.script.ScriptFuture;
import workbook.util.ScrollUtil;
import workbook.view.result.Result;
import workbook.view.result.ResultRenderer;

/**
 * A view that displays a single cell in a worksheets, with a prompt, command, and result view.
 */
public class Cell {
	private final Composite parent;
	private final Label prompt;
	private final StyledText command;
	
	private final ResultRenderer resultRenderer;
	
	private final List<Runnable> upCallbacks = new ArrayList<>();
	private final List<Runnable> downCallbacks = new ArrayList<>();
	private final List<Runnable> insertCallbacks = new ArrayList<>();
	private final List<Runnable> deleteCallbacks = new ArrayList<>();
	private final List<Runnable> runCallbacks = new ArrayList<>();
	private final List<Runnable> runAllCallbacks = new ArrayList<>();
	private final List<Runnable> notifyCallbacks = new ArrayList<>();
	
	private final Result result;
	private Function<String, ScriptFuture<Object>> executeFunction = null;
	private Function<String, String> completionFunction = null;
	
	private String previousCommandText = "";
	private Point previousSelection = new Point(0, 0);
	
	public Cell(Composite parent, ScrolledComposite scrolledComposite, ResultRenderer resultRenderer, Cell cellAbove) {
		this.parent = parent;
		this.resultRenderer = resultRenderer;
		
		Display display = parent.getDisplay();
		
		prompt = addLabel(parent, ">>>");
		command = new StyledText(parent, SWT.NONE);
		result = new Result(parent, resultRenderer);
		
		if(cellAbove != null) {
			prompt.moveBelow(cellAbove.result.asComposite());
			command.moveBelow(prompt);
			result.asComposite().moveBelow(command);
		}
		
		command.setFont(FontList.consolas10);
		
		command.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent event) {
				previousCommandText = command.getText();
				
				// Don't allow changes that are only newlines to allow key listener to handle these.
				if(event.text.matches("[\r\n]+")) {
					event.doit = false;
				}
			}
		});
		
		command.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				parent.layout();
			}
		});
		
		command.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent event) {
				command.setSelection(command.getCharCount());
			}
		});
		
		command.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				if(event.keyCode == SWT.CR && event.stateMask == SWT.NONE) {
					// Run run callbacks on return.
					runCallbacks.forEach(Runnable::run);
					evaluate(() -> notifyCallbacks.forEach(Runnable::run));
				} else if(event.keyCode == SWT.CR && event.stateMask == SWT.CONTROL) {
					// Run runAll callbacks on ctrl+return.
					runAllCallbacks.forEach(Runnable::run);
					event.doit = false;
				} else if(event.keyCode == SWT.CR && event.stateMask == SWT.SHIFT) {
					// Insert newline into this cell on shift+return.
					String text = command.getText();
					int x = command.getCaretOffset();
					command.setText(text.substring(0, x) + "\n" + text.substring(x));
					command.setCaretOffset(x + 1);
					
					// Scroll down if necessary.
					scrolledComposite.setMinSize(parent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					ScrollUtil.scrollVerticallyTo(scrolledComposite, getBounds());
				} else if(event.keyCode == SWT.INSERT) {
					// Run insert callbacks on insert.
					event.doit = false;
					insertCallbacks.forEach(Runnable::run);
					evaluate(() -> notifyCallbacks.forEach(Runnable::run));
				} else if(event.keyCode == 'a' && event.stateMask == SWT.CONTROL) {
					selectAll();
				}
			}
		});
		
		command.addCaretListener(new CaretListener() {
			public void caretMoved(CaretEvent event) {
				Display.getCurrent().asyncExec(() -> previousSelection = command.getSelection());
			}
		});
		
		command.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				Display.getCurrent().asyncExec(() -> previousSelection = command.getSelection());
			}
		});
		
		command.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if(event.character == SWT.BS && previousCommandText.isEmpty()) {
					// Run delete callbacks on backspace, if the previous text value was empty.
					deleteCallbacks.forEach(Runnable::run);
				} else if(event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN) {
					int previousCaretOffset = (event.keyCode == SWT.ARROW_UP) ? previousSelection.x : previousSelection.y;
					if(previousCaretOffset >= 0 && previousCaretOffset <= command.getCharCount()) {
						if(event.keyCode == SWT.ARROW_UP && command.getLineAtOffset(previousCaretOffset) == 0) {
							// Return up callback.
							upCallbacks.forEach(Runnable::run);
						} else if(event.keyCode == SWT.ARROW_DOWN) {
							// Return down callback.
							if(command.getLineAtOffset(previousCaretOffset) == command.getLineCount() - 1) {
								downCallbacks.forEach(Runnable::run);
							}
						}
					}
				}
				
				if(event.keyCode != SWT.TAB) {
					// Dismiss completion on any character except tab.
					completionFunction.apply(null);
				}
				
				if(!command.isDisposed()) {
					previousCommandText = command.getText();
				}
			}
		});
		
		command.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {
				// Insert tab completion on tab when cursor is at the end of the command control.
				if(event.keyCode == SWT.TAB && event.stateMask == 0) {
					if(command.getSelection().x == command.getText().length()) {
						String completedText = completionFunction.apply(command.getText());
						command.setText(completedText);
						command.setSelection(command.getText().length());
						event.doit = false;
					}
				}
			}
		});
		
		command.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				// Scroll to make this cell visible when any character is entered.
				int x = command.getCaret().getLocation().x + command.getBounds().x + getBounds().x;
				ScrollUtil.scrollHorizontallyTo(scrolledComposite, new Rectangle(x - 50, 0, 100, 0));
			}
		});
		
		command.setLayoutData(new GridDataBuilder().fillHorizontal().build());
		result.asComposite().setLayoutData(new GridDataBuilder().horizontalSpan(2).build());
		
		prompt.setLayoutData(new GridDataBuilder().verticalAlignment(SWT.TOP).build());
		
		prompt.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		prompt.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));

		command.setForeground(display.getSystemColor(SWT.COLOR_DARK_CYAN));
	}
	
	public void evaluate(Runnable callback) {
		if(command.getText().trim().isEmpty()) {
			result.clear();
			callback.run();
			parent.pack();
		} else {
			result.setLoading();
			
			parent.pack();
			
			executeFunction.apply(command.getText()).thenAcceptAlways(resultObject -> {
				Display.getDefault().asyncExec(() -> {
					result.setValue(resultObject, () -> {
						Display.getDefault().asyncExec(() -> {
							callback.run();
							parent.pack();
							parent.layout();
						});
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
	
	public void setCompletionFunction(Function<String, String> completionFunction) {
		this.completionFunction = completionFunction;
	}
	
	public void addUpCallback(Runnable callback) {
		upCallbacks.add(callback);
	}
	
	public void addDownCallback(Runnable callback) {
		downCallbacks.add(callback);
	}
	
	public void addInsertCallback(Runnable callback) {
		insertCallbacks.add(callback);
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
	
	public void addNotifyCallbacks(Runnable callback) {
		notifyCallbacks.add(callback);
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
	
	public void selectAll() {
		command.selectAll();
		previousSelection = command.getSelection();
	}
	
	public void setFocus() {
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