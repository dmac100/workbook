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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;

import workbook.layout.GridLayoutBuilder;
import workbook.script.ScriptController;
import workbook.script.ScriptFuture;

public class WorksheetTabbedView implements TabbedView {
	private final ScrolledComposite scrolledCellsComposite;
	private final Composite cellsComposite;
	private final ScriptController scriptController;
	
	private final Completion completion = new Completion();
	
	private final List<Cell> cells = new ArrayList<>();
	
	private Function<String, ScriptFuture<Object>> executeFunction;
	
	public WorksheetTabbedView(Composite parent, ScriptController scriptController) {
		this.scriptController = scriptController;
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
	
	private Cell addPrompt() {
		final Cell cell = new Cell(cellsComposite, scriptController);
		
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
				scrollTo(cells.get(index).getBounds());
			}
		});
		
		cell.addDownCallback(new Runnable() {
			public void run() {
				int index = cells.indexOf(cell) + 1;
				index = Math.min(index, cells.size() - 1);
				cells.get(index).setFocus();
				scrollTo(cells.get(index).getBounds());
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
					scrollTo(cells.get(index).getBounds());
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
					scrollTo(cells.get(index).getBounds());
				}
			}
		});
		
		cell.addRunAllCallback(new Runnable() {
			public void run() {
				for(Cell prompt:cells) {
					prompt.evaluate(false);
				}
			}
		});
		
		cells.add(cell);
		cell.setFocus();
		pack();
		
		scrollTo(cell.getBounds());
		
		return cell;
	}
	
	private void selectLast() {
		if(!cells.isEmpty()) {
			int index = cells.size() - 1;
			cells.get(index).setFocus();
			scrollTo(cells.get(index).getBounds());
		}
	}
	
	public void setExecuteFunction(Function<String, ScriptFuture<Object>> executeFunction) {
		this.executeFunction = executeFunction;
	}
	
	private void scrollTo(Rectangle bounds) {
		Rectangle area = scrolledCellsComposite.getClientArea();
		Point origin = scrolledCellsComposite.getOrigin();
		if(origin.x > bounds.x) {
			origin.x = Math.max(0, bounds.x);
		}
		if(origin.y > bounds.y) {
			origin.y = Math.max(0, bounds.y);
		}
		if(origin.x + area.width < bounds.x + bounds.width) {
			origin.x = Math.max(0, bounds.x + bounds.width - area.width);
		}
		if(origin.y + area.height < bounds.y + bounds.height) {
			origin.y = Math.max(0, bounds.y + bounds.height - area.height);
		}
		scrolledCellsComposite.setOrigin(origin);
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