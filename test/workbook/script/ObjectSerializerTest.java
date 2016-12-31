package workbook.script;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ObjectSerializerTest {
	private ObjectSerializer serializer = new ObjectSerializer();
	
	@Test
	public void roundTripString() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("a", "b");
		
		String data = serializer.serialize(map);
		
		Map<String, Object> returned = serializer.deserialize(data);
		
		assertEquals("b", returned.get("a"));
	}
	
	@Test
	public void roundTripNull() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("a", null);
		
		String data = serializer.serialize(map);
		
		Map<String, Object> returned = serializer.deserialize(data);
		
		assertTrue(returned.containsKey("a"));
	}
	
	@Test
	public void roundTripDate() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("a", LocalDate.of(2010, 2, 1));
		
		String data = serializer.serialize(map);
		
		Map<String, Object> returned = serializer.deserialize(data);
		
		assertEquals(2010, ((LocalDate) returned.get("a")).getYear());
		assertEquals(2, ((LocalDate) returned.get("a")).getMonth().getValue());
		assertEquals(1, ((LocalDate) returned.get("a")).getDayOfMonth());
	}
}
