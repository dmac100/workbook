package workbook.util;

import java.util.List;
import java.util.function.Predicate;

public class TypeUtil {
	/**
	 * Returns whether a list is actually a List of the type tested by the predicate.
	 */
	public static boolean isListOf(Object list, Predicate<Object> predicate) {
		if(!(list instanceof List)) {
			return false;
		}
		
		if(((List) list).isEmpty()) {
			return false;
		}
		
		for(Object item:(List) list) {
			if(!predicate.test(item)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Returns whether a list is actually a List of the type tested by the predicate,
	 * including the empty list.
	 */
	public static boolean isListOfOrEmpty(Object list, Predicate<Object> predicate) {
		if(!(list instanceof List)) {
			return false;
		}
		
		for(Object item:(List) list) {
			if(!predicate.test(item)) {
				return false;
			}
		}
		
		return true;
	}
}