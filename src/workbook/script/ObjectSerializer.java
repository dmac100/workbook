package workbook.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Serializes objects to and from Strings for saving to a file.
 */
public class ObjectSerializer {
	/**
	 * Returns map serialized to a String.
	 */
	public String serialize(Map<String, Object> map) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		
		for(Entry<String, Object> entry:map.entrySet()) {
			if(entry.getValue() instanceof Serializable) {
				// Serialize name and value of each variable.
				objectOutputStream.writeObject(entry.getKey());
				objectOutputStream.writeObject(entry.getValue());
			}
		}
		
		// Mark end of variables.
		objectOutputStream.writeObject(null);

		return wrap(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
	}
	
	/**
	 * Returns map deserialized from a String.
	 */
	public Map<String, Object> deserialize(String data) throws IOException, ClassNotFoundException {
		String encoded = data.replaceAll("\\s", "");
		if(encoded.isEmpty()) return new HashMap<>();
		
		byte[] globalsData = Base64.getDecoder().decode(encoded);
		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(globalsData));
		
		Map<String, Object> map = new HashMap<>();
		
		// Read name and value of each variable.
		while(true) {
			String name = (String) objectInputStream.readObject();
			if(name == null) {
				return map;
			}
			Object value = objectInputStream.readObject();
			map.put(name, value);
		}
	}
	
	/**
	 * Returns the string with wrapped lines.
	 */
	private static String wrap(String string) {
		int w = 60;
		StringBuilder wrapped = new StringBuilder();
		for(int c = 0; c < string.length(); c += w) {
			wrapped.append(string.substring(c, Math.min(string.length(), c + w)));
			wrapped.append("\n");
		}
		return wrapped.toString();
	}
}
