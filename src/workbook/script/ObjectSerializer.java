package workbook.script;

import java.io.IOException;
import java.util.Map;

import com.thoughtworks.xstream.XStream;

/**
 * Serializes objects to and from Strings for saving to a file.
 */
public class ObjectSerializer {
	/**
	 * Returns map serialized to a String.
	 */
	public String serialize(Map<String, Object> map) throws IOException {
		return new XStream().toXML(map);
	}
	
	/**
	 * Returns map deserialized from a String.
	 */
	public Map<String, Object> deserialize(String data) throws IOException, ClassNotFoundException {
		return (Map<String, Object>) new XStream().fromXML(data);
	}
}
