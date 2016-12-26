package workbook.view;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.jdom2.Element;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import workbook.event.MajorRefreshEvent;
import workbook.event.MinorRefreshEvent;
import workbook.layout.GridLayoutBuilder;
import workbook.script.ScriptController;
import workbook.script.ScriptFuture;
import workbook.util.ScrollUtil;
import workbook.view.result.ResultRenderer;

/**
 * A view that displays a worksheet allowing the entering of commands and the inline display of their results.
 */
public class WorksheetTabbedView implements TabbedView {
	private final EventBus eventBus;
	private final ScrolledComposite scrolledCellsComposite;
	private final ScriptController scriptController;
	private final Composite cellsComposite;
	private final ResultRenderer resultRenderer;
	
	private Function<String, ScriptFuture<Object>> executeFunction;
	private String executeFunctionName;
	
	private final Completion completion = new Completion();
	private final List<Cell> cells = new ArrayList<>();
	
	private Cell focusedCell = null;
	
	/**
	 * Creates a worksheet that evaluates against the given evaluation function name.
	 */
	public WorksheetTabbedView(Composite parent, EventBus eventBus, ScriptController scriptController, ResultRenderer resultRenderer, String executeFunctionName) {
		this(parent, eventBus, scriptController, resultRenderer);
		setExecuteFunctionName(executeFunctionName);
	}
	
	/**
	 * Creates a worksheet that evaluates against a custom evaluation function.
	 */
	public WorksheetTabbedView(Composite parent, EventBus eventBus, ScriptController scriptController, ResultRenderer resultRenderer, Function<String, Object> executeFunction) {
		this(parent, eventBus, scriptController, resultRenderer);
		
		this.executeFunction = command -> scriptController.exec(() -> executeFunction.apply(command));
	}
	
	/**
	 * Creates a worksheet that evaluates against the script controller evaluation function.
	 */
	public WorksheetTabbedView(Composite parent, EventBus eventBus, ScriptController scriptController, ResultRenderer resultRenderer) {
		this.eventBus = eventBus;
		this.scriptController = scriptController;
		this.resultRenderer = resultRenderer;
		this.executeFunction = scriptController::eval;
		
		Display display = parent.getDisplay();
		
		parent.setLayout(new FillLayout());
		
		scrolledCellsComposite = ScrollUtil.createScrolledComposite(parent);
		cellsComposite = (Composite) scrolledCellsComposite.getContent();
		cellsComposite.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		cellsComposite.setLayout(new GridLayoutBuilder()
			.numColumns(2)
			.marginWidth(5)
			.marginHeight(3)
			.horizontalSpacing(2)
			.verticalSpacing(2)
			.build()
		);
		
		cellsComposite.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent event) {
				selectLast();
			}
		});

		addPrompt(null);
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
	/**
	 * Sets the name of the function to execute when evaluating a command, or null to use the default script evaluation.
	 */
	private void setExecuteFunctionName(String executeFunctionName) {
		this.executeFunctionName = executeFunctionName;
		
		if(executeFunctionName == null || executeFunctionName.isEmpty()) {
			this.executeFunction = scriptController::eval;
		} else {
			this.executeFunction = command -> scriptController.evalMethodCall(executeFunctionName, Arrays.asList(command));
		}
	}
	
	@Subscribe
	public void onMinorRefresh(MinorRefreshEvent event) {
	}
	
	@Subscribe
	public void onMajorRefresh(MajorRefreshEvent event) {
		refresh();
	}
	
	private Cell addPrompt(Cell cellAbove) {
		final Cell cell = new Cell(cellsComposite, scrolledCellsComposite, resultRenderer, cellAbove);
		cell.setExecuteFunction(command -> executeFunction.apply(command));
		
		cell.addNotifyCallbacks(() -> {
			eventBus.post(new MinorRefreshEvent(this));
			pack();
			scrollToFocusedCell();
		});
		
		cell.setCompletionFunction(text -> {
			if(text == null) {
				completion.dismiss();
				return null;
			} else {
				completion.setHistory(cells.stream().map(Cell::getCommand).collect(toList()));
				String completedText = completion.getCompletion(text);
				return completedText;
			}
		});
		
		cell.addUpCallback(new Runnable() {
			public void run() {
				int index = cells.indexOf(cell) - 1;
				index = Math.max(index, 0);
				focusCell(cells.get(index));
			}
		});
		
		cell.addDownCallback(new Runnable() {
			public void run() {
				int index = cells.indexOf(cell) + 1;
				index = Math.min(index, cells.size() - 1);
				focusCell(cells.get(index));
			}
		});
		
		cell.addInsertCallback(new Runnable() {
			public void run() {
				addPrompt(cell);
				scrollToFocusedCell();
				focusCell(cells.get(cells.indexOf(cell) + 1));
			}
		});
		
		cell.addDeleteCallback(new Runnable() {
			public void run() {
				if(cells.size() > 1) {
					int index = cells.indexOf(cell);
					cells.remove(index);
					index = Math.max(0, index - 1);
					cell.dispose();
					pack();
					focusCell(cells.get(index));
				}
			}
		});
		
		cell.addRunCallback(new Runnable() {
			public void run() {
				if(cell == cells.get(cells.size() - 1)) {
					addPrompt(null);
					focusCell(cells.get(cells.size() - 1));
				} else {
					focusCell(cells.get(cells.indexOf(cell) + 1));
				}
			}
		});
		
		cell.addRunAllCallback(new Runnable() {
			public void run() {
				refresh();
				eventBus.post(new MinorRefreshEvent(this));
			}
		});

		if(cellAbove == null) {
			cells.add(cell);
		} else {
			cells.add(cells.indexOf(cellAbove) + 1, cell);
		}
		
		cell.setFocus();
		pack();
		
		ScrollUtil.scrollVerticallyTo(scrolledCellsComposite, cell.getBounds());
		
		return cell;
	}

	private void focusCell(Cell cell) {
		this.focusedCell = cell;
		focusedCell.selectAll();
		scrollToFocusedCell();
	}
	
	private void scrollToFocusedCell() {
		if(focusedCell != null) {
			focusedCell.setFocus();
			
			Rectangle bounds = focusedCell.getBounds();
			bounds.x = 0;
			bounds.width = 0;
			
			ScrollUtil.scrollTo(scrolledCellsComposite, bounds);
		}
	}
	
	private void refresh() {
		// Run evaluate on all cells, and post minor refresh event when all have been evaluated.
		Display.getDefault().asyncExec(new Runnable() {
			int count = cells.size();
			
			public void run() {
				for(Cell cell:cells) {
					cell.evaluate(() -> {
						if(--count == 0) {
							eventBus.post(new MinorRefreshEvent(this));
						}
					});
				}
			}
		});
	}
	
	private void selectLast() {
		if(!cells.isEmpty()) {
			int index = cells.size() - 1;
			cells.get(index).setFocus();
			ScrollUtil.scrollVerticallyTo(scrolledCellsComposite, cells.get(index).getBounds());
		}
	}
	
	public void pack() {
		cellsComposite.pack();
		scrolledCellsComposite.setMinSize(cellsComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	public Control getControl() {
		return scrolledCellsComposite;
	}
	
	private void clear() {
		for(Cell cell:cells) {
			cell.dispose();
		}
		cells.clear();
	}

	public void serialize(Element element) {
		if(this.executeFunctionName != null) {
			Element executeFunctionNameElement = new Element("ExecuteFunctionName");
			executeFunctionNameElement.setText(executeFunctionName);
			element.addContent(executeFunctionNameElement);
		}
		
		for(Cell cell:cells) {
			Element command = new Element("Command");
			command.setText(cell.getCommand());
			element.addContent(command);
		}
	}

	public void deserialize(Element element) {
		clear();
		
		setExecuteFunctionName(element.getChildText("ExecuteFunctionName"));
		
		for(Element command:element.getChildren("Command")) {
			Cell cell = addPrompt(null);
			cell.setCommand(command.getText());
		}
	}
	
	public void createMenu(Menu menu) {
		MenuItem clearItem = new MenuItem(menu, SWT.NONE);
		clearItem.setText("Clear");
		clearItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				clear();
				addPrompt(null);
			}
		});
		
		MenuItem setExecuteFunctionItem = new MenuItem(menu, SWT.NONE);
		setExecuteFunctionItem.setText("Set Execute Function...");
		setExecuteFunctionItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				String executeFunction = InputDialog.open(Display.getDefault().getActiveShell(), "Execute Function", "Execute Function");
				if(executeFunction != null) {
					setExecuteFunctionName(executeFunction);
				}
			}
		});
	}
}