package workbook.editor.reference;

import static org.junit.Assert.*;

import org.junit.Test;

import workbook.editor.reference.Reference;
import workbook.editor.reference.ScriptPropertyReference;
import workbook.script.ScriptController;

public class ScriptPropertyReferenceTest {
	@Test
	public void test() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		Object object = scriptController.eval("x = { a: 1, b: 2 }; x", x -> {}, x -> {}).get();
		
		Reference reference = new ScriptPropertyReference(scriptController, object, "a");
		
		assertEquals(1, reference.get().get());
		
		reference.set(2).get();
		
		assertEquals(2, reference.get().get());
	}
}
