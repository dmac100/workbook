package workbook.editor.reference;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import workbook.script.JavascriptEngine;
import workbook.script.ScriptController;

public class GlobalVariableReferenceTest {
	@Test
	public void test() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		scriptController.addEngine("Javascript", new JavascriptEngine());
		scriptController.setScriptType("Javascript");
		scriptController.eval("x = 2").get();
		
		GlobalVariableReference reference = new GlobalVariableReference(scriptController, "x");
		
		assertEquals(2, reference.get().get());
		
		reference.set(3);
		assertEquals(3, reference.get().get());
	}
}
