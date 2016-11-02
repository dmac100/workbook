package workbook.editor.reference;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import workbook.script.ScriptController;

public class ConstantReferenceTest {
	@Test
	public void simple() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		
		Reference reference = new ConstantReference(scriptController, "a");
		
		assertEquals("a", reference.get().get());
		
		reference.set("b").get();
		
		assertEquals("a", reference.get().get());
	}
}