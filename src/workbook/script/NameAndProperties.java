package workbook.script;

import java.util.HashMap;
import java.util.Map;

/**
 * An item that has a name and list of properties as key-value pairs.
 */
public class NameAndProperties {
	private final String name;
	private final Map<String, String> properties;
	
	public NameAndProperties(String name, Map<String, String> properties) {
		this.name = name;
		this.properties = new HashMap<>(properties);
	}
	
	public String getName() {
		return name;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
	
	public String toString() {
		return name + " - " + properties;
	}
}
