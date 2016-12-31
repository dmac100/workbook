package workbook.script;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * Serializes objects to and from Strings for saving to a file.
 */
public class ObjectSerializer {
	private final XStream xstream;

	public ObjectSerializer() {
		xstream = new XStream(new DomDriver()) {
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				return new MapperWrapper(next) {
					public boolean shouldSerializeMember(Class definedIn, String fieldName) {
						try {
							return definedIn != Object.class || realClass(fieldName) != null;
						} catch(CannotResolveClassException e) {
							return false;
						}
					}
				};
			}
		};
		
		xstream.ignoreUnknownElements();
	}

	/**
	 * Returns map serialized to a String.
	 */
	public String serialize(Map<String, Object> map) throws IOException {
		Map<String, Object> serializable = new HashMap<>();
		map.forEach((k, v) -> {
			if(v == null || v instanceof Serializable) {
				serializable.put(k, v);
			}
		});
		return xstream.toXML(serializable);
	}

	/**
	 * Returns map deserialized from a String.
	 */
	public Map<String, Object> deserialize(String data) throws IOException, ClassNotFoundException {
		return (Map<String, Object>) xstream.fromXML(data);
	}
}
