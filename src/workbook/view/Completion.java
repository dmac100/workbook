package workbook.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tab completion class to complete text based on a history of entered commands.
 */
public class Completion {
	private String completionPrefix = "";
	private String lastCompletion = "";
	private Set<String> words = new TreeSet<>();

	/**
	 * Sets the history of commands to complete based on.
	 */
	public void setHistory(List<String> history) {
		words.clear();
		for(String s:history) {
			for(String w:s.split("\\W+")) {
				words.add(w);
			}
		}
	}

	/**
	 * Returns the next String to complete based on the current string.
	 */
	public String getCompletion(String text) {
		String prefix = text.replaceAll("\\w*$", "");
		String suffix = text.substring(prefix.length());
		
		if(completionPrefix.isEmpty()) {
			completionPrefix = suffix;
		}
		
		List<String> list = createCompletionList(words, completionPrefix);
		
		boolean useNextCompletion = lastCompletion.isEmpty();
		
		// Find the completion occuring after the last completion, or the first completion if there isn't any last completion.
		for(String word:list) {
			if(word.toLowerCase().startsWith(completionPrefix.toLowerCase())) {
				if(useNextCompletion) {
					lastCompletion = word;
					return prefix + word;
				}
				
				if(word.equals(lastCompletion)) {
					useNextCompletion = true;
				}
			}
		}
		
		return text;
	}

	/**
	 * Returns a list of words that will be completed in order.
	 */
	private List<String> createCompletionList(Set<String> words, String completionPrefix) {
		Set<String> wordsWithoutPrefix = new TreeSet<>(words);
		wordsWithoutPrefix.remove(completionPrefix);
		
		List<String> list = new ArrayList<>();
		list.addAll(wordsWithoutPrefix);
		list.add(completionPrefix);
		list.addAll(wordsWithoutPrefix);
		return list;
	}

	public void dismiss() {
		completionPrefix = "";
		lastCompletion = "";
		words.clear();
	}
}
