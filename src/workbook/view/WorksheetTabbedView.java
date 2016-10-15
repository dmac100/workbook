package workbook.view;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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

public class WorksheetTabbedView implements TabbedView {
	private final EventBus eventBus;
	private final ScrolledComposite scrolledCellsComposite;
	private final Composite cellsComposite;
	private final ScriptController scriptController;
	private final ResultRenderer resultRenderer;
	
	private Function<String, ScriptFuture<Object>> executeFunction;
	
	private final Completion completion = new Completion();
	private final List<Cell> cells = new ArrayList<>();
	
	public WorksheetTabbedView(Composite parent, EventBus eventBus, ScriptController scriptController, ResultRenderer resultRenderer) {
		this.eventBus = eventBus;
		this.scriptController = scriptController;
		this.resultRenderer = resultRenderer;
		
		Display display = parent.getDisplay();
		
		parent.setLayout(new FillLayout());
		
		scrolledCellsComposite = createScrolledComposite(parent);
		cellsComposite = (Composite) scrolledCellsComposite.getContent();
		cellsComposite.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		cellsComposite.setLayout(new GridLayoutBuilder()
			.numColumns(2)
			.marginWidth(5)
			.marginHeight(3)
			.horizontalSpacing(0)
			.verticalSpacing(2)
			.build()
		);
		
		cellsComposite.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent event) {
				selectLast();
			}
		});

		addPrompt();
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
	private static ScrolledComposite createScrolledComposite(Composite parent) {
		final ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		final Composite composite = new Composite(scrolledComposite, SWT.NONE);
		scrolledComposite.setContent(composite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent event) {
				scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});
		return scrolledComposite;
	}
	
	@Subscribe
	public void onMinorRefresh(MinorRefreshEvent event) {
	}
	
	@Subscribe
	public void onMajorRefresh(MajorRefreshEvent event) {
		refresh();
	}
	
	private Cell addPrompt() {
		final Cell cell = new Cell(cellsComposite, scrolledCellsComposite, scriptController, resultRenderer);
		cell.setExecuteFunction(command -> executeFunction.apply(command));
		
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
				cells.get(index).setFocus();
				ScrollUtil.scrollVerticallyTo(scrolledCellsComposite, cells.get(index).getBounds());
			}
		});
		
		cell.addDownCallback(new Runnable() {
			public void run() {
				int index = cells.indexOf(cell) + 1;
				index = Math.min(index, cells.size() - 1);
				cells.get(index).setFocus();
				ScrollUtil.scrollVerticallyTo(scrolledCellsComposite, cells.get(index).getBounds());
			}
		});
		
		cell.addDeleteCallback(new Runnable() {
			public void run() {
				if(cells.size() > 1) {
					int index = cells.indexOf(cell);
					cells.remove(index);
					index = Math.max(0, index - 1);
					cells.get(index).setFocus();
					cell.dispose();
					pack();
					ScrollUtil.scrollVerticallyTo(scrolledCellsComposite, cells.get(index).getBounds());
				}
			}
		});
		
		cell.addRunCallback(new Runnable() {
			public void run() {
				if(cell == cells.get(cells.size() - 1)) {
					addPrompt();
				} else {
					int index = cells.indexOf(cell) + 1;
					cells.get(index).setFocus();
					ScrollUtil.scrollVerticallyTo(scrolledCellsComposite, cells.get(index).getBounds());
				}
				eventBus.post(new MinorRefreshEvent());
			}
		});
		
		cell.addRunAllCallback(new Runnable() {
			public void run() {
				refresh();
				eventBus.post(new MinorRefreshEvent());
			}
		});
		
		cells.add(cell);
		cell.setFocus();
		pack();
		
		ScrollUtil.scrollVerticallyTo(scrolledCellsComposite, cell.getBounds());
		
		return cell;
	}
	
	private void refresh() {
		Display.getDefault().asyncExec(() -> {
			for(Cell prompt:cells) {
				prompt.evaluate(false);
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
	
	public void setExecuteFunction(Function<String, ScriptFuture<Object>> executeFunction) {
		this.executeFunction = executeFunction;
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
		for(Cell cell:cells) {
			Element command = new Element("Command");
			command.setText(cell.getCommand());
			element.addContent(command);
		}
	}

	public void deserialize(Element element) {
		clear();
		
		for(Element command:element.getChildren("Command")) {
			Cell cell = addPrompt();
			cell.setCommand(command.getText());
		}
	}
}