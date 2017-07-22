package workbook.view;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.jdom2.Element;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import workbook.event.MajorRefreshEvent;
import workbook.event.OutputEvent;

/**
 * A view that displays the console output.
 */
public class ConsoleTabbedView implements TabbedView {
	private final Composite parent;
	private final StyledText text;
	
	public ConsoleTabbedView(Composite parent, EventBus eventBus) {
		this.parent = parent;
		
		text = new StyledText(parent, SWT.WRAP | SWT.V_SCROLL);
		text.setFont(FontList.MONO_NORMAL);
		text.setEditable(false);
		
		text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if((event.stateMask & SWT.CTRL) > 0) {
					if(event.keyCode == 'a') {
						selectAll();
					}
				}
			}
		});
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
	@Subscribe
	public void onOutput(OutputEvent event) {
		text.getDisplay().asyncExec(() -> {
			addOutput(event.getOutput());
			addError(event.getError());
		});
	}
	
	@Subscribe
	public void onMajorRefresh(MajorRefreshEvent event) {
		text.getDisplay().asyncExec(() -> {
			clear();
		});
	}
	
	public void selectAll() {
		text.setSelection(0, text.getText().length());
	}
	
	private void addOutput(String output) {
		text.append(WordUtils.wrap(String.valueOf(output), 1000, "\n", true));
		text.setTopIndex(text.getLineCount() - 1);
	}
	
	private void addError(String error) {
		int start = text.getCharCount();
		
		String wrappedOutput = WordUtils.wrap(String.valueOf(error), 1000, "\n", true);
		
		text.append(wrappedOutput);
		text.setTopIndex(text.getLineCount() - 1);
		
		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = wrappedOutput.length();
		styleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		
		text.replaceStyleRanges(start, wrappedOutput.length(), new StyleRange[] { styleRange });
	}
	
	public void clear() {
		text.setText("");
	}

	public Control getControl() {
		return text;
	}

	public void serialize(Element element) {
	}

	public void deserialize(Element element) {
	}

	public void createMenu(Menu menu) {
		MenuItem clearItem = new MenuItem(menu, SWT.NONE);
		clearItem.setText("Clear");
		clearItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				clear();
			}
		});
	}
}