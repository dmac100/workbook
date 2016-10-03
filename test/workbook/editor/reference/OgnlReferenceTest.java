package workbook.editor.reference;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import workbook.editor.reference.OgnlReference;
import workbook.script.ScriptController;

public class OgnlReferenceTest {
	@Test
	public void getSetScriptObject() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		scriptController.eval("x = { a: { b: 1 } }", x -> {}, x -> {}).get();
		
		OgnlReference reference = new OgnlReference(scriptController, "x.a.b");
		
		assertEquals(1, reference.get().get());
		
		reference.set(2);
		assertEquals(2, reference.get().get());
	}
	
	@Test
	public void getSetScriptArray() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		scriptController.eval("x = [1, 2, 3]", x -> {}, x -> {}).get();
		
		OgnlReference reference = new OgnlReference(scriptController, "x[\"1\"]");
		
		assertEquals(2, reference.get().get());
		
		reference.set(3);
		assertEquals(3, reference.get().get());
	}
	
	@Test
	public void getSetJavaArray() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		scriptController.setVariable("x", new int[] { 1, 2, 3 }).get();
		
		OgnlReference reference = new OgnlReference(scriptController, "x[1]");
		
		assertEquals(2, reference.get().get());
		
		reference.set(3);
		assertEquals(3, reference.get().get());
	}
}
