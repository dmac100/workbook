package script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ScriptTest {
	private final Script script = new Script();
	
	@Test
	public void eval() {
		Object result = script.eval("1 + 1");
		assertEquals(2, result);
	}
	
	@Test
	public void evalWithCallbackFunctions() {
		List<NameAndProperties> values = script.evalWithCallbackFunctions("rect({a: 1, b: 2}); line({a: 3});", Arrays.asList("rect", "line"), x -> {}, x -> {});
		
		assertEquals(2, values.size());
		
		assertEquals("rect", values.get(0).getName());
		assertEquals("1", values.get(0).getProperties().get("a"));
		assertEquals("2", values.get(0).getProperties().get("b"));
		
		assertEquals("line", values.get(1).getName());
		assertEquals("3", values.get(1).getProperties().get("a"));
	}
	
	@Test
	public void evalWithCallbackFunctions_accessGlobals() {
		script.eval("x = 3");
		
		List<NameAndProperties> values = script.evalWithCallbackFunctions("line({a: x});", Arrays.asList("line"), x -> {}, x -> {});
		
		assertEquals("3", values.get(0).getProperties().get("a"));
	}
	
	@Test
	public void evalWithCallbackFunctions_callbackScope() {
		List<NameAndProperties> values = script.evalWithCallbackFunctions("line({a: x});", Arrays.asList("line"), x -> {}, x -> {});
		
		assertNull(script.eval("line"));
	}
}
