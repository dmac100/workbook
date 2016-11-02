package workbook.editor.reference;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import workbook.script.ScriptController;

public class ListItemReferenceTest {
	@Test
	public void simple() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		List<String> object = new ArrayList<>(Arrays.asList("a", "b"));
		
		Reference reference = new ListItemReference(scriptController, object, 0);
		
		assertEquals("a", reference.get().get());
		
		reference.set("b").get();
		
		assertEquals("b", reference.get().get());
	}
	
	@Test
	public void typeConversion() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		List<Integer> object = new ArrayList<>(Arrays.asList(1, 2));
		
		Reference reference = new ListItemReference(scriptController, object, 0);
		
		assertEquals(1, reference.get().get());
		
		reference.set("2").get();
		
		assertEquals(2, reference.get().get());
	}
}
