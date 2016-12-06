package workbook.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A comparator that compares String after grouping on runs of digits, so that numbers are
 * ordered in numerical order.
 */
public class NaturalOrderComparator implements Comparator<String> {
	private static final Pattern groupPattern = Pattern.compile("([1-9][0-9]*)|([^ 0])");
	
	private static class CompareBuilder {
		private int compare = 0;
		
		public void compare(BigInteger a, BigInteger b) {
			if(compare == 0) {
				compare = a.compareTo(b);
			}
		}
		
		public void compare(String a, String b) {
			if(compare == 0) {
				compare = a.compareTo(b);
			}
		}
		
		public void compare(int a, int b) {
			if(compare == 0) {
				compare = Integer.compare(a, b);
			}
		}
		
		public int get() {
			return compare;
		}
	}
	
	public int compare(String a, String b) {
		CompareBuilder compare = new CompareBuilder();
	
		List<String> groupa = getGroups(a);
		List<String> groupb = getGroups(b);
		
		for(int i = 0; i < Math.min(groupa.size(), groupb.size()); i++) {
			String sa = groupa.get(i);
			String sb = groupb.get(i);
			
			if(Character.isDigit(sa.charAt(0)) && Character.isDigit(sb.charAt(0))) {
				compare.compare(new BigInteger(sa), new BigInteger(sb));
			} else {
				compare.compare(sa, sb);
			}
		}

		compare.compare(groupa.size(), groupb.size());		
		
		String sa = extractGroup(a, "(0*)$");
		String sb = extractGroup(b, "(0*)$");

		compare.compare(sa.length(), sb.length());
		
		return compare.get();		
	}
	
	private static String extractGroup(String s, String pattern) {
		Matcher m = Pattern.compile(pattern).matcher(s);
		if(m.find()) {
			return (m.groupCount() > 0) ? m.group(1) : m.group(0);
		}
		return null;
	}
	
	private static List<String> getGroups(String a) {
		List<String> groups = new ArrayList<>();
		
		Matcher matcher = groupPattern.matcher(a);
		while(matcher.find()) {
			groups.add(matcher.group(0));
		}
		
		return groups;
	}
}
