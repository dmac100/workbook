package workbook.view;

import java.util.ArrayList;
import java.util.List;

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
import workbook.view.ansi.AnsiParser;
import workbook.view.ansi.AnsiStyle;
import workbook.view.ansi.ParseResult;
import workbook.view.canvas.ColorCache;

/**
 * A view that displays the console output.
 */
public class ConsoleTabbedView implements TabbedView {
	private final Composite parent;
	private final StyledText text;
	private final ColorCache colorCache;
	
	private List<StyleRange> styles = new ArrayList<StyleRange>();
	private AnsiStyle lastStyle;
	
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
		
		colorCache = new ColorCache(Display.getCurrent());
		getControl().addDisposeListener(colorCache);
		
		clear();
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
		addWithStyles(wrap(output));
		text.setTopIndex(text.getLineCount() - 1);
	}
	
	private void addError(String error) {
		int start = text.getCharCount();
		
		String wrappedOutput = wrap(error);
		
		text.append(wrappedOutput);
		text.setTopIndex(text.getLineCount() - 1);
		
		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = wrappedOutput.length();
		styleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		
		text.replaceStyleRanges(start, wrappedOutput.length(), new StyleRange[] { styleRange });
	}
	
	/**
	 * Adds text to the console by extracting any new styles from it, appending
	 * the text, and applying the styles.
	 */
	private void addWithStyles(String newText) {
		ParseResult parseResult = new AnsiParser().parseText(lastStyle, newText);
		
		int offset = text.getCharCount();
		text.append(parseResult.getNewText());
		
		for(AnsiStyle ansiStyle:parseResult.getStyleRanges()) {
			StyleRange style = new StyleRange(styles.get(styles.size() - 1));
			style.start = offset + ansiStyle.start;
			style.length = ansiStyle.length;
			
			if(ansiStyle.foreground == null) {
				style.foreground = null;
			} else {
				style.foreground = colorCache.getColor(ansiStyle.foreground);
			}
			if(ansiStyle.background == null) {
				style.background = null;
			} else {
				style.background = colorCache.getColor(ansiStyle.background);
			}
			
			if(ansiStyle.bold) style.fontStyle |= SWT.BOLD;
			if(ansiStyle.italic) style.fontStyle |= SWT.ITALIC;
			if(ansiStyle.underline) style.underline = true;
			if(ansiStyle.doubleUnderline) style.underlineStyle = SWT.UNDERLINE_DOUBLE;
			
			styles.add(style);
			lastStyle = ansiStyle;
		}
		
		text.setStyleRanges(styles.toArray(new StyleRange[styles.size()]));
	}
	
	private static String wrap(String output) {
		StringBuilder s = new StringBuilder();
		for(String line:output.split("\\r?\\n", -1)) {
			if(s.length() != 0) {
				s.append("\n");
			}
			String wrappedLine = WordUtils.wrap(String.valueOf(line), 1000, "\n", true);
			s.append(wrappedLine);
		}
		return s.toString();
	}

	public void clear() {
		text.setText("");
		
		styles.clear();
		styles.add(new StyleRange());
		
		lastStyle = new AnsiStyle();
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