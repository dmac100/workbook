package workbook.view.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.google.common.base.Optional;

import syntaxhighlight.ParseResult;
import syntaxhighlight.Style;
import syntaxhighlight.Theme;
import syntaxhighlighter.SyntaxHighlighterParser;
import syntaxhighlighter.brush.Brush;
import workbook.view.FontList;
import workbook.view.canvas.ColorCache;

/**
 * A view to edit text with extra editing features to allow easier editing of sourcecode.
 */
public class EditorText {
	private final StyledText styledText;
	private final ColorCache colorCache;
	private final StyledTextCompletion completion;
	private final EditFunctions editFunctions;
	
	private Brush brush = null;
	private Map<String, Brush> brushes = new HashMap<>();
	private final Theme theme = new ThemeSublime();
	private StyleRange[] syntaxHighlightingRanges = new StyleRange[0];
	
	public EditorText(Composite parent) {
		colorCache = new ColorCache(Display.getCurrent());
		
		styledText = new StyledText(parent, SWT.V_SCROLL);
		styledText.setMargins(2, 1, 2, 1);
		styledText.setTabs(4);
		
		editFunctions = new EditFunctions(styledText);
		completion = new StyledTextCompletion(styledText);
		
		styledText.setFont(FontList.MONO_NORMAL);

		styledText.addDisposeListener(colorCache);

		// Disable traverse to allow tab and shift+tab for selection indentation.
		styledText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {
				if(event.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
					event.doit = false;
				}
			}
		});

		styledText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				normalizeNewlines();
				updateSyntaxHighlightingRanges();
				refreshStyle();
				refreshLineStyles();
			}
		});
		
		styledText.addCaretListener(new CaretListener() {
			public void caretMoved(CaretEvent event) {
				// Delay refreshing line style to ensure the new line count is used when deleting lines.
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						refreshStyle();
						refreshLineStyles();
					}
				});
			}
		});
		
		styledText.addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if(event.character == '\r' || event.character == '\n') {
					if((event.stateMask & SWT.CTRL) > 0) {
					} else {
						// Preserve indentation on newline.
						newline();
					}
					event.doit = false;
				}
				
				// Indent on tab.
				if(event.character == '\t') {
					if((event.stateMask & SWT.SHIFT) > 0) {
						unindentSelection();
						event.doit = false;
					} else {
						Point selection = styledText.getSelection();
						if(selection.x != selection.y) {
							indentSelection();
							event.doit = false;
						} else if(completion.canComplete()) {
							completion.complete();
							event.doit = false;
						}
					}
				}
				
				// Scroll on Ctrl+Up/Down.
				if((event.stateMask & SWT.CTRL) > 0) {
					if(event.keyCode == SWT.ARROW_UP) {
						styledText.setTopPixel(styledText.getTopPixel() - 20);
					} else if(event.keyCode == SWT.ARROW_DOWN) {
						styledText.setTopPixel(styledText.getTopPixel() + 20);
					}
				}
				
				// Move lines on Alt+Up/Down.
				if((event.stateMask & SWT.ALT) > 0) {
					if(event.keyCode == SWT.ARROW_UP) {
						moveSelectedLinesUp();
					} else if(event.keyCode == SWT.ARROW_DOWN) {
						moveSelectedLinesDown();
					}
				}
				
				// Smart home.
				if(event.keyCode == SWT.HOME) {
					if((event.stateMask & SWT.SHIFT) > 0) {
						smartHome(true);
						event.doit = false;
					} else if(event.stateMask == 0) {
						smartHome(false);
						event.doit = false;
					}
				}
		
				if(event.character > 0 || event.keyCode == SWT.HOME) {
					styledText.showSelection();
				}
			}
		});
		
		styledText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if((event.stateMask & SWT.CTRL) > 0) {
					if(event.keyCode == 'a') {
						editFunctions.selectAll();
					} else if(event.keyCode == 'd') {
						deleteLine();
					} else if(event.keyCode == 'f') {
						find();
					}
				}
			}
		});
		
		styledText.setFocus();
		
		refreshStyle();
		refreshLineStyles();
		
		refreshContextMenu();
	}
	
	private void newline() {
		int line = styledText.getLineAtOffset(styledText.getCaretOffset());
		String indent = styledText.getLine(line).replaceAll("\\S.*", "");
		
		styledText.insert("\n" + indent);
		if(line + 1 < styledText.getLineCount()) {
			styledText.setCaretOffset(styledText.getOffsetAtLine(line + 1) + indent.length());
		}
	}
	
	private void indentSelection() {
		Point selection = styledText.getSelection();
		
		int startLine = styledText.getLineAtOffset(selection.x);
		int endLine = styledText.getLineAtOffset(selection.y);
		
		StringBuilder text = new StringBuilder(styledText.getText());
		
		for(int line = endLine; line >= startLine; line--) {
			int offset = styledText.getOffsetAtLine(line);
			text.replace(offset, offset, "\t");
		}
		
		styledText.setText(text.toString());

		int lines = endLine - startLine + 1;
		styledText.setSelection(selection.x, selection.y + lines);
	}
	
	private void unindentSelection() {
		Point selection = styledText.getSelection();
		
		int startLine = styledText.getLineAtOffset(selection.x);
		int endLine = styledText.getLineAtOffset(selection.y);
		
		int firstLineCharactersRemoved = 0;
		int totalCharactersRemoved = 0;
		
		StringBuilder text = new StringBuilder(styledText.getText());
		
		for(int line = endLine; line >= startLine; line--) {
			int offset = styledText.getOffsetAtLine(line);
			String lineText = styledText.getLine(line);
			
			int charactersToRemove = getUnindentSize(lineText);
			if(line == startLine) {
				firstLineCharactersRemoved += charactersToRemove;
			}
			totalCharactersRemoved += charactersToRemove;
			
			text.delete(offset, offset + charactersToRemove);
		}
		
		styledText.setText(text.toString());
		
		int newSelectionStart = Math.max(styledText.getOffsetAtLine(startLine), selection.x - firstLineCharactersRemoved);
		styledText.setSelection(newSelectionStart, selection.y - totalCharactersRemoved);
	}
	
	/**
	 * Returns the number of characters needed to be removed from the beginning of line to unindent it.
	 */
	private int getUnindentSize(String line) {
		int indentSize = 0;
		for(int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if(!Character.isWhitespace(c)) {
				return i;
			}
			indentSize += (c == '\t') ? 4 : 1;
			if(indentSize >= 4 || !Character.isWhitespace(c)) {
				return i + 1;
			}
		}
		return line.length();
	}
	
	private void smartHome(boolean selectText) {
		int selectionStart = getSelectionStart();
		
		int offset = styledText.getCaretOffset();
		int line = styledText.getLineAtOffset(offset);
		int lineStartOffset = styledText.getOffsetAtLine(line);
		
		int horizontalOffset = offset - lineStartOffset;
		
		String lineText = styledText.getLine(line);
		
		int firstNonWhiteSpace = lineText.replaceAll("\\S.*", "").length();

		if(selectText) {
			if(horizontalOffset != firstNonWhiteSpace) {
				styledText.setSelection(selectionStart, lineStartOffset + firstNonWhiteSpace);
			} else {
				styledText.setSelection(selectionStart, lineStartOffset);
			}
		} else {
			if(horizontalOffset != firstNonWhiteSpace) {
				styledText.setCaretOffset(lineStartOffset + firstNonWhiteSpace);
			} else {
				styledText.setCaretOffset(lineStartOffset);
			}
		}
	}
	
	/**
	 * Moves all the selected lines up one line, and selects them all.
	 * Moves the current line if there is no selection.
	 */
	private void moveSelectedLinesUp() {
		Point selection = styledText.getSelection();
		int startLine = styledText.getLineAtOffset(selection.x);
		int endLine = styledText.getLineAtOffset(selection.y);

		if(endLine > startLine) {
			// If the selection ends at the beginning of a line, use the previous line instead.
			endLine = styledText.getLineAtOffset(selection.y - 1);
		}
		
		if(startLine > 0) {
			for(int line = startLine; line <= endLine; line++) {
				swapLineWithNext(line - 1);
			}
			
			styledText.setSelection(styledText.getOffsetAtLine(startLine - 1), getEndOfLineOffset(endLine - 1));
		}
	}
	
	/**
	 * Moves all the selected lines down one line, and selects them all.
	 * Moves the current line if there is no selection.
	 */
	private void moveSelectedLinesDown() {
		Point selection = styledText.getSelection();
		int startLine = styledText.getLineAtOffset(selection.x);
		int endLine = styledText.getLineAtOffset(selection.y);
		
		if(endLine > startLine) {
			// If the selection ends at the beginning of a line, use the previous line instead.
			endLine = styledText.getLineAtOffset(selection.y - 1);
		}
		
		if(endLine + 1 < styledText.getLineCount()) {
			for(int line = endLine; line >= startLine; line--) {
				swapLineWithNext(line);
			}
			
			styledText.setSelection(styledText.getOffsetAtLine(startLine + 1), getEndOfLineOffset(endLine + 1));
		}
	}
	
	private void swapLineWithNext(int line) {
		boolean atLastLine = (line + 2 >= styledText.getLineCount());
		
		if(atLastLine) {
			styledText.append("\n");
		}
		
		int offset1 = styledText.getOffsetAtLine(line);
		int offset2 = styledText.getOffsetAtLine(line + 1);
		int offset3 = styledText.getOffsetAtLine(line + 2);
		
		String line1 = styledText.getTextRange(offset1, offset2 - offset1);
		String line2 = styledText.getTextRange(offset2, offset3 - offset2);
		
		styledText.replaceTextRange(offset2, offset3 - offset2, line1);
		styledText.replaceTextRange(offset1, offset2 - offset1, line2);
		
		if(atLastLine) {
			styledText.replaceTextRange(styledText.getCharCount() - 1, 1, "");
		}
	}
	
	private int getEndOfLineOffset(int line) {
		return styledText.getOffsetAtLine(line) + styledText.getLine(line).length();
	}

	/**
	 * Returns the starting offset of the current selection, or the caret position if there is no selection.
	 */
	private int getSelectionStart() {
		Point range = styledText.getSelectionRange();
		int offset1 = range.x;
		int offset2 = range.x + range.y;
		
		// The caret is at the end, so the offset that is not the caret position.
		if(styledText.getCaretOffset() == offset1) {
			return offset2;
		} else {
			return offset1;
		}
	}

	public void deleteLine() {
		int line = styledText.getLineAtOffset(styledText.getCaretOffset());
		
		int lineStart = styledText.getOffsetAtLine(line);
		int lineEnd = getEndOfLineOffset(line);
		
		if(lineEnd + 1 > styledText.getCharCount()) {
			styledText.append("\n");
		}
		
		styledText.replaceTextRange(lineStart, lineEnd - lineStart + 1, "");
	}
	
	/**
	 * Replace all newline characters with "\n".
	 */
	private void normalizeNewlines() {
		int offset = styledText.getCaretOffset();
		String text = styledText.getText();
		
		if(text.contains("\r")) {
			offset -= offset - replaceNewLines(text.substring(0, offset)).length();
			text = replaceNewLines(text);
			styledText.setText(text);
			styledText.setCaretOffset(offset);
			styledText.showSelection();
		}
	}
	
	/**
	 * Replace all newline characters with "\n".
	 */
	private static String replaceNewLines(String text) {
		return text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
	}
	

	/**
	 * Refresh the line styles including the current line highlight, and line numbers.
	 */
	private void refreshLineStyles() {
		int line = styledText.getLineAtOffset(styledText.getCaretOffset());
		int maxLine = styledText.getLineCount();
		
		int lineCountWidth = Math.max(String.valueOf(maxLine).length(), 3);
		
		// Update line numbers.
		StyleRange style = new StyleRange();
		style.metrics = new GlyphMetrics(0, 0, lineCountWidth * 8 + 5);
		style.foreground = colorCache.getColor(70, 80, 90);
		Bullet bullet = new Bullet(ST.BULLET_NUMBER, style);
		styledText.setLineBullet(0, maxLine, null);
		styledText.setLineBullet(0, maxLine, bullet);
		
		// Update current line highlight.
		int lineCount = styledText.getContent().getLineCount();
		styledText.setLineBackground(0, lineCount, colorCache.getColor(theme.getBackground()));
		styledText.setLineBackground(line, 1, colorCache.getColor(47, 48, 42));
	}
	
	/**
	 * Updates the syntax highlighting styles.
	 */
	private void updateSyntaxHighlightingRanges() {
		if(brush == null) return;
		
		// Set syntax highlighting.
		SyntaxHighlighterParser parser = new SyntaxHighlighterParser(brush);
		List<ParseResult> results = filterResults(parser.parse(null, styledText.getText()));
		
		StyleRange[] styleRanges = new StyleRange[results.size()];
		for(int i = 0; i < styleRanges.length; i++) {
			ParseResult result = results.get(i);
			
			StyleRange range = new StyleRange();
			range.start = result.getOffset();
			range.length = result.getLength();
			range.fontStyle = SWT.NORMAL;
			
			Style foregroundStyle = theme.getStyles().get(result.getStyleKeys().get(0));
			if(foregroundStyle != null) {
				java.awt.Color foreground = foregroundStyle.getColor();
				range.foreground = colorCache.getColor(foreground);
			} else {
				java.awt.Color normal = theme.getPlain().getColor();
				range.foreground = colorCache.getColor(normal);
			}
			
			styleRanges[i] = range;
		}
		
		this.syntaxHighlightingRanges = styleRanges;
	}
	
	/**
	 * Refresh character style including foreground, background, syntax highlighting, and bracket highlighting.
	 */
	private void refreshStyle() {
		// Set background color.
		java.awt.Color background = theme.getBackground();
		styledText.setBackground(colorCache.getColor(background));
		
		// Set foreground color.
		java.awt.Color normal = theme.getPlain().getColor();
		styledText.setForeground(colorCache.getColor(normal));
		
		// Set syntax highlighting.
		styledText.setStyleRanges(syntaxHighlightingRanges);
		
		// Set bracket highlighting.
		if(styledText.getCaretOffset() > 0) {
			String text = styledText.getText();
			
			BracketMatcher bracketMatcher = new BracketMatcher();
			Optional<Integer> match = bracketMatcher.getMatchingParen(text, styledText.getCaretOffset() - 1);
			if(match.isPresent()) {
				int x = match.get();

				StyleRange range = new StyleRange();
				range.start = x;
				range.length = 1;
				range.foreground = getForegroundAt(x);
				range.borderStyle = SWT.BORDER_SOLID;
				range.borderColor = colorCache.getColor(150, 150, 150);
				styledText.setStyleRange(range);
			}
		}
		
		// Set matching word highlighting
		String selected = styledText.getSelectionText();
		if(selected.matches("[\\w]+")) {
			String text = styledText.getText();
			
			int index = -1;
			while((index = text.indexOf(selected, index + 1)) >= 0) {
				StyleRange range = new StyleRange();
				range.start = index;
				range.length = selected.length();
				range.foreground = getForegroundAt(index);
				range.borderStyle = SWT.BORDER_SOLID;
				range.borderColor = colorCache.getColor(150, 150, 150);
				styledText.setStyleRange(range);
			}
		}
	}
	
	private Color getForegroundAt(int index) {
		for(StyleRange range:syntaxHighlightingRanges) {
			if(range.start <= index && range.start + range.length > index) {
				return range.foreground;
			}
		}
		return null;
	}
	
	/**
	 * Returns the list of ParseResults so that it doesn't contain overlapping offsets.
	 */
	private List<ParseResult> filterResults(List<ParseResult> results) {
		List<ParseResult> filtered = new ArrayList<>();
		
		int lastIndex = -1;
		for(ParseResult result:results) {
			if(result.getOffset() <= lastIndex) {
				continue;
			}
			
			filtered.add(result);
			
			lastIndex = result.getOffset() + result.getLength();
		}
		
		return filtered;
	}

	public String getText() {
		return styledText.getText();
	}

	public void setText(String string) {
		if(!replaceNewLines(styledText.getText()).equals(replaceNewLines(string))) {
			Point selection = styledText.getSelection();
			
			styledText.setText(string);

			styledText.setSelection(selection);
		}
	}
	
	public Control getControl() {
		return styledText;
	}
	
	public void find() {
		new FindDialog(Display.getDefault().getActiveShell(), styledText).open();
	}

	public void convertTabsToSpaces() {
		styledText.setText(styledText.getText().replaceAll("\t", "    "));
	}
	
	public Brush getBrush() {
		return brush;
	}
	
	public void setBrush(Brush brush) {
		this.brush = brush;
		updateSyntaxHighlightingRanges();
		refreshStyle();
		refreshLineStyles();
	}
	
	public void setBrushes(Map<String, Brush> brushes) {
		this.brushes = brushes;
		refreshContextMenu();
	}
	
	private void refreshContextMenu() {
		if(!brushes.isEmpty()) {
			Menu menu = new Menu(styledText);
			
			MenuItem syntaxMenuItem = new MenuItem(menu, SWT.CASCADE);
			syntaxMenuItem.setText("Syntax");
			Menu syntaxMenu = new Menu(menu);
			syntaxMenuItem.setMenu(syntaxMenu);

			brushes.forEach((name, brush) -> {
				MenuItem item = new MenuItem(syntaxMenu, SWT.NONE);
				item.setText(name);
				item.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent event) {
						setBrush(brush);
					}
				});
			});
			
			if(styledText.getMenu() != null) {
				styledText.getMenu().dispose();
			}
			styledText.setMenu(menu);
		}
	}

	public StyledText getStyledText() {
		return styledText;
	}
}
