package workbook.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Completion {
	private String completionPrefix = "";
	private String lastCompletion = "";
	private Set<String> words = new TreeSet<>();

	public void setHistory(List<String> history) {
		words.clear();
		for(String s:history) {
			for(String w:s.split("\\W+")) {
				words.add(w);
			}
		}
	}

	public String getCompletion(String text) {
		String prefix = text.replaceAll("\\w*$", "");
		String suffix = text.substring(prefix.length());
		
		if(completionPrefix.isEmpty()) {
			completionPrefix = suffix;
		}
		
		Set<String> wordsWithoutPrefix = new TreeSet<>(words);
		wordsWithoutPrefix.remove(completionPrefix);
		
		List<String> list = new ArrayList<>();
		list.addAll(wordsWithoutPrefix);
		list.add(completionPrefix);
		list.addAll(wordsWithoutPrefix);
		
		boolean startCompletion = false;
		for(String word:list) {
			if(word.toLowerCase().startsWith(completionPrefix.toLowerCase())) {
				if(startCompletion) {
					lastCompletion = word;
					return prefix + word;
				}
				
				if(word.equals(lastCompletion) || lastCompletion.isEmpty()) {
					if(lastCompletion.isEmpty()) {
						lastCompletion = word;
						return prefix + word;
					}
					startCompletion = true;
				}
			}
		}
		
		return text;
	}

	public void dismiss() {
		completionPrefix = "";
		lastCompletion = "";
		words.clear();
	}
}
