package workbook.editor.reference;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import workbook.editor.reference.MapPropertyReference;
import workbook.editor.reference.Reference;
import workbook.script.ScriptController;

public class MapPropertyReferenceTest {
	@Test
	public void simple() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		Map<String, Object> object = new HashMap<>();
		object.put("a", 1);
		object.put("b", 2);
		
		Reference reference = new MapPropertyReference(scriptController, object, "a");
		
		assertEquals(1, reference.get().get());
		
		reference.set(2).get();
		
		assertEquals(2, reference.get().get());
	}
	
	@Test
	public void typeConversion() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		Map<String, Integer> object = new HashMap<>();
		object.put("a", 1);
		object.put("b", 2);
		
		Reference reference = new MapPropertyReference(scriptController, object, "a");
		
		assertEquals(1, reference.get().get());
		
		reference.set("2").get();
		
		assertEquals(2, reference.get().get());
	}
}