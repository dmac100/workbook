package workbook.view;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class CompletionTest {
	@Test
	public void getCompletion_noCompletion() {
		Completion completion = new Completion();
		completion.setHistory(Arrays.asList(
			"dc",
			"da",
			"db"
		));
		assertEquals("a", completion.getCompletion("a"));
		assertEquals("a", completion.getCompletion("a"));
	}
	
	@Test
	public void getCompletion() {
		Completion completion = new Completion();
		completion.setHistory(Arrays.asList(
			"ac",
			"aa",
			"bb"
		));
		assertEquals("aa", completion.getCompletion("a"));
		assertEquals("ac", completion.getCompletion("aa"));
		assertEquals("a", completion.getCompletion("ac"));
		assertEquals("aa", completion.getCompletion("a"));
	}
	
	@Test
	public void getCompletion_ignoreCase() {
		Completion completion = new Completion();
		completion.setHistory(Arrays.asList(
			"ac",
			"Aa",
			"bb"
		));
		assertEquals("Aa", completion.getCompletion("a"));
		assertEquals("ac", completion.getCompletion("Aa"));
	}
	
	@Test
	public void getCompletion_historyWords() {
		Completion completion = new Completion();
		completion.setHistory(Arrays.asList(
			"ab ac",
			"bd be",
			"aab bbb"
		));
		assertEquals("aab", completion.getCompletion("a"));
		assertEquals("ab", completion.getCompletion("aab"));
		assertEquals("ac", completion.getCompletion("ab"));
		assertEquals("a", completion.getCompletion("ac"));
	}
	
	@Test
	public void getCompletion_completionWords() {
		Completion completion = new Completion();
		completion.setHistory(Arrays.asList(
			"ab ac",
			"bd be",
			"aab bbb"
		));
		assertEquals("dd aab", completion.getCompletion("dd a"));
		assertEquals("dd ab", completion.getCompletion("dd aac"));
		assertEquals("dd ac", completion.getCompletion("dd ab"));
		assertEquals("dd a", completion.getCompletion("dd ab"));
	}
	
	@Test
	public void getCompletion_multiline() {
		Completion completion = new Completion();
		completion.setHistory(Arrays.asList(
			"abc"
		));
		assertEquals("abc\n", completion.getCompletion("abc\n"));
	}
	
	@Test
	public void getCompletion_multilineCompletion() {
		Completion completion = new Completion();
		completion.setHistory(Arrays.asList(
			"abc"
		));
		assertEquals("abc\nabc", completion.getCompletion("abc\na"));
	}
}