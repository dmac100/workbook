package workbook.view.canvas;

import java.util.LinkedHashMap;
import java.util.Map;

public class CanvasItem {
	private final String name;
	private final Map<String, String> values;
	
	public CanvasItem(String name, Map<String, String> values) {
		this.name = name;
		this.values = new LinkedHashMap<>(values);
		
		setDefaultValues(this.values);
	}
	
	private static void setDefaultValues(Map<String, String> values) {
		setDefaultColorValues(values, "fill", "red");
		setDefaultColorValues(values, "stroke", "darkgrey");
		values.put("opacity", "1");
	}
	
	private static void setDefaultColorValues(Map<String, String> values, String prefix, String defaultValue) {
		String color = values.get(prefix);
		if(color == null) {
			setComponentColorValues(values, prefix, defaultValue);
		} else {
			setComponentColorValues(values, prefix, color);
		}
	}

	/**
	 * Splits the color value into separate red, green, and blue properties with the given prefix.
	 */
	private static void setComponentColorValues(Map<String, String> values, String prefix, String value) {
		int[] rgb = new int[] { 200, 100, 100 };
		if(value.equals("red")) rgb = new int[] { 200, 100, 100 };
		if(value.equals("green")) rgb = new int[] { 100, 200, 100 };
		if(value.equals("blue")) rgb = new int[] { 100, 100, 200 };
		if(value.equals("yellow")) rgb = new int[] { 200, 200, 100 };
		if(value.equals("magenta")) rgb = new int[] { 200, 100, 200 };
		if(value.equals("cyan")) rgb = new int[] { 100, 200, 200 };
		if(value.equals("white")) rgb = new int[] { 255, 255, 255 };
		if(value.equals("lightgrey")) rgb = new int[] { 200, 200, 200 };
		if(value.equals("grey")) rgb = new int[] { 150, 150, 150 };
		if(value.equals("darkgrey")) rgb = new int[] { 50, 50, 50 };
		if(value.equals("black")) rgb = new int[] { 0, 0, 0 };
		if(value.matches("#[0-9a-fA-F]{3}")) {
			rgb = new int[] {
				Integer.parseInt(value.substring(1, 2) + value.substring(1, 2), 16),
				Integer.parseInt(value.substring(2, 3) + value.substring(2, 3), 16),
				Integer.parseInt(value.substring(3, 4) + value.substring(3, 4), 16),
			};
		} else if(value.toLowerCase().matches("#[0-9a-fA-F]{6}")) {
			rgb = new int[] {
				Integer.parseInt(value.substring(1, 2) + value.substring(2, 3), 16),
				Integer.parseInt(value.substring(3, 4) + value.substring(4, 5), 16),
				Integer.parseInt(value.substring(5, 6) + value.substring(6, 7), 16),
			};
		}
		values.put(prefix + "-red", String.valueOf(rgb[0]));
		values.put(prefix + "-green", String.valueOf(rgb[1]));
		values.put(prefix + "-blue", String.valueOf(rgb[2]));
	}

	public String getName() {
		return name;
	}
	
	public boolean hasProperty(String name) {
		return values.containsKey(name);
	}
	
	public String getProperty(String name) {
		return values.get(name);
	}
	
	public void setProperty(String name, String value) {
		values.put(name, value);
	}
}
