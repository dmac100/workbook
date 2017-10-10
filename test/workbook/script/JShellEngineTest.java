package workbook.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.junit.Test;

public class JShellEngineTest {
	private final JShellEngine script = new JShellEngine();
	
	@Test
	public void eval() {
		Object result = script.eval("1 + 1");
		assertEquals(2, result);
	}
	
	@Test
	public void eval_scriptOutput() {
		List<String> list = new ArrayList<>();
		System.setOut(new PrintStream(new LineReader(list::add).getOutputStream()));
		Object result = script.eval("System.out.println(\"a\")");
		assertEquals(Arrays.asList("a"), list);
	}
	
	@Test
	public void eval_javaOutput() {
		List<String> list = new ArrayList<>();
		System.setOut(new PrintStream(new LineReader(list::add).getOutputStream()));
		Object result = script.eval("System.out.println(\"a\")");
		assertEquals(Arrays.asList("a"), list);
	}
	
	@Test
	public void evalWithCallbackFunctions() {
		script.eval("import java.util.*;");
		List<NameAndProperties> values = script.evalWithCallbackFunctions("rect(Map.of(\"a\", 1, \"b\", 2)); line(Map.of(\"a\", 3));", Arrays.asList("rect", "line"));

		assertEquals(2, values.size());
		
		assertEquals("rect", values.get(0).getName());
		assertEquals("1", values.get(0).getProperties().get("a"));
		assertEquals("2", values.get(0).getProperties().get("b"));
		
		assertEquals("line", values.get(1).getName());
		assertEquals("3", values.get(1).getProperties().get("a"));
	}
	
	@Test
	public void evalWithCallbackFunctions_accessGlobals() {
		script.eval("import java.util.*;");
		script.eval("int x = 3;");
		List<NameAndProperties> values = script.evalWithCallbackFunctions("line(Map.of(\"a\", x));", Arrays.asList("line"));
		
		assertEquals("3", values.get(0).getProperties().get("a"));
	}
	
	@Test
	public void evalMethodCall() {
		Object result = script.evalMethodCall("String.valueOf", Arrays.asList("123"));
		
		assertEquals("123", result);
	}
	
	@Test
	public void isIterable_true() throws ScriptException {
		Object object = script.eval("java.util.Arrays.asList(1, 2)");
		assertTrue(script.isIterable(object));
	}
	
	@Test
	public void isIterable_false() throws ScriptException {
		Object object = script.eval("3");
		assertFalse(script.isIterable(object));
	}
	
	@Test
	public void iterateObject() throws ScriptException {
		List<Object> list = new ArrayList<>();
		Object object = script.eval("java.util.Arrays.asList(1, 2, 3)");
		
		script.iterateObject(object, list::add);
		
		assertEquals(Arrays.asList(1, 2, 3), list);
	}
	
	@Test
	public void setVariable() throws ScriptException {
		script.setVariable("a", "b");
		assertEquals("b", script.getVariable("a"));
	}
	
	@Test
	public void getPropertyMap_get() throws ScriptException {
		Object object = script.eval("java.util.Map.of(\"a\", \"b\")");
		
		Map<Object, Object> map = script.getPropertyMap(object);
		
		assertEquals("b", map.get("a"));
	}
	
	@Test
	public void getPropertyMap_set() throws ScriptException {
		Object object = script.eval("new java.util.HashMap(java.util.Map.of(\"a\", \"b\"))");
		Map<Object, Object> map = script.getPropertyMap(object);
		
		map.put("a", "c");
		map.put("b", "d");
		
		assertEquals("c", script.getPropertyMap(object).get("a"));
		assertEquals("d", script.getPropertyMap(object).get("b"));
	}
	
	@Test
	public void isScriptObject_true() throws ScriptException {
	}
	
	@Test
	public void isScriptObject_false() throws ScriptException {
		Object object = script.eval("10");
		assertFalse(script.isScriptObject(object));
	}
}