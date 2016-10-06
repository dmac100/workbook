package workbook.view.text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyledText;

/**
 * Performs tab-completion on a StyledText when the complete method is called. Completes based
 * on words that already exist in the control. Repeating a completion will cycle through a
 * completion list until the caret position or text are changed.
 */
public class Completion {
	private final StyledText styledText;
	
	private int lastCaretOffset = -1;
	private String lastText = "";
	
	private List<String> completions;
	private int completionIndex;
	private int wordStartOffset;

	public Completion(StyledText styledText) {
		this.styledText = styledText;
		dismissCompletions();
	}

	/**
	 * Performs a tab-completion, updating the text and caret position as necessary.
	 */
	public void complete() {
		if(styledText.getCharCount() == 0) return;
		
		int offset = styledText.getCaretOffset();
		
		if(offset != lastCaretOffset || !styledText.getText().equals(lastText)) {
			dismissCompletions();
		}
		
		if(completions == null) {
			fillCompletions();
		}
		
		if(!completions.isEmpty()) {
			String completion = completions.get(completionIndex);
			
			styledText.replaceTextRange(wordStartOffset, offset - wordStartOffset, completion);
			styledText.setCaretOffset(wordStartOffset + completion.length());
			
			completionIndex = (completionIndex + 1) % completions.size();
		}
		
		lastCaretOffset = styledText.getCaretOffset();
		lastText = styledText.getText();
		
		return;
	}
	
	/**
	 * Returns whether the caret is in a position where completion can occur, such
	 * as after a word character.
	 */
	public boolean canComplete() {
		int offset = styledText.getCaretOffset();
		if(offset == 0) return false;
		
		String text = styledText.getText(offset-1, offset-1);
		return text.matches("[\\w_]");
	}

	/**
	 * Dismisses the current completion list.
	 */
	public void dismissCompletions() {
		completions = null;
		completionIndex = 0;
		wordStartOffset = 0;
	}

	/**
	 * Fills completions, completionIndex, and wordStartOffset based on the currently possible
	 * completions.
	 */
	private void fillCompletions() {
		int offset = styledText.getCaretOffset();
		
		String prefix;
		String suffix;
		
		if(offset > 0) {
			prefix = styledText.getText(0, offset - 1);
		} else {
			prefix = "";
		}
		
		if(offset < styledText.getCharCount()) {
			suffix = styledText.getText(offset, styledText.getCharCount() - 1);
		} else {
			suffix = "";
		}
		
		completionIndex = 0;
		completions = new ArrayList<>();

		String completionPrefix = match(prefix, "[\\w_]+$");
		if(completionPrefix == null) {
			return;
		}
		
		wordStartOffset = offset - completionPrefix.length();
		
		String[] prefixWords = prefix.split("[^\\w_]+");
		String[] suffixWords = suffix.split("[^\\w_]+");
		reverse(prefixWords);
		reverse(suffixWords);
		
		Set<String> words = new LinkedHashSet<>();
		for(String word:prefixWords) words.add(word);
		for(String word:suffixWords) words.add(word);
		
		for(String word:words) {
			if(word.length() > completionPrefix.length()) {
				if(word.toLowerCase().startsWith(completionPrefix.toLowerCase())) {
					completions.add(word);
				}
			}
		}
		
		completions.add(completionPrefix);
	}
	
	/**
	 * Reverses an array in-place.
	 */
	private static void reverse(Object[] a) {
		for(int i = 0; i < a.length / 2; i++) {
			int j = a.length - i - 1;
			Object t = a[i];
			a[i] = a[j];
			a[j] = t;
		}
	}

	/**
	 * Returns the first match in s for the pattern, or null
	 * if there is no match.
	 */
	private static String match(String s, String pattern) {
		Matcher matcher = Pattern.compile(pattern).matcher(s);
		if(matcher.find()) {
			if(matcher.groupCount() > 0) {
				return matcher.group(1);
			} else {
				return matcher.group();
			}
		} else {
			return null;
		}
	}
}
