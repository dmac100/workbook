package workbook.script;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
	
	public boolean equals(Object object) {
		if(object instanceof NameAndProperties) {
			NameAndProperties other = (NameAndProperties) object;
			return Objects.equals(name, other.name) && Objects.equals(properties, other.properties);
		}
		return false;
	}
	
	public int hashCode() {
		return Objects.hash(name, properties);
	}
}
