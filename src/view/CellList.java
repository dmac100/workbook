package view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

import layout.GridLayoutBuilder;
import script.ScriptFuture;

public class CellList implements View {
	private final ScrolledComposite scrolledCellsComposite;
	private final Composite cellsComposite;
	
	private final List<Cell> prompts = new ArrayList<>();
	
	private Function<String, ScriptFuture<Object>> executeFunction;
	
	public CellList(Composite parent) {
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
	
	private void addPrompt() {
		final Cell prompt = new Cell(cellsComposite);
		
		prompt.setExecuteFunction(command -> executeFunction.apply(command));
		
		prompt.addUpCallback(new Runnable() {
			public void run() {
				int index = prompts.indexOf(prompt) - 1;
				if(index >= 0) {
					prompts.get(index).setFocus();
					scrollTo(prompts.get(index).getBounds());
				}
			}
		});
		
		prompt.addDownCallback(new Runnable() {
			public void run() {
				int index = prompts.indexOf(prompt) + 1;
				if(index < prompts.size()) {
					prompts.get(index).setFocus();
					scrollTo(prompts.get(index).getBounds());
				}
			}
		});
		
		prompt.addDeleteCallback(new Runnable() {
			public void run() {
				if(prompts.size() > 1) {
					int index = prompts.indexOf(prompt);
					prompts.remove(index);
					index = Math.max(0, index - 1);
					prompts.get(index).setFocus();
					prompt.dispose();
					pack();
					scrollTo(prompts.get(index).getBounds());
				}
			}
		});
		
		prompt.addRunCallback(new Runnable() {
			public void run() {
				if(prompt == prompts.get(prompts.size() - 1)) {
					addPrompt();
				} else {
					int index = prompts.indexOf(prompt) + 1;
					prompts.get(index).setFocus();
					scrollTo(prompts.get(index).getBounds());
				}
			}
		});
		
		prompt.addRunAllCallback(new Runnable() {
			public void run() {
				for(Cell prompt:prompts) {
					prompt.evaluate(false);
				}
			}
		});
		
		prompts.add(prompt);
		prompt.setFocus();
		pack();
		
		scrollTo(prompt.getBounds());
	}
	
	private void selectLast() {
		if(!prompts.isEmpty()) {
			int index = prompts.size() - 1;
			prompts.get(index).setFocus();
			scrollTo(prompts.get(index).getBounds());
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
}