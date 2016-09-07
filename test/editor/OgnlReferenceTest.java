package editor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import script.Script;

public class OgnlReferenceTest {
	@Test
	public void getSetScriptObject() {
		Script script = new Script();
		script.eval("x = { a: { b: 1 } }");
		
		OgnlReference reference = new OgnlReference(script.getVariableMap(), "x.a.b");
		
		assertEquals(1, reference.get());
		
		reference.set(2);
		assertEquals(2, reference.get());
	}
	
	@Test
	public void getSetScriptArray() {
		Script script = new Script();
		script.eval("x = [1, 2, 3]");
		
		OgnlReference reference = new OgnlReference(script.getVariableMap(), "x[\"1\"]");
		
		assertEquals(2, reference.get());
		
		reference.set(3);
		assertEquals(3, reference.get());
	}
	
	@Test
	public void getSetJavaArray() {
		Script script = new Script();
		script.addVariable("x", new int[] { 1, 2, 3 });
		
		OgnlReference reference = new OgnlReference(script.getVariableMap(), "x[1]");
		
		assertEquals(2, reference.get());
		
		reference.set(3);
		assertEquals(3, reference.get());
	}
}
