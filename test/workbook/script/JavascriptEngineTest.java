package workbook.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.junit.Test;

import workbook.script.JavascriptEngine;
import workbook.script.NameAndProperties;

public class JavascriptEngineTest {
	private final JavascriptEngine script = new JavascriptEngine();
	
	@Test
	public void eval() {
		Object result = script.eval("1 + 1");
		assertEquals(2, result);
	}
	
	@Test
	public void eval_output() {
		List<String> list = new ArrayList<>();
		Object result = script.eval("print('a')", list::add, list::add);
		assertEquals(Arrays.asList("a"), list);
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
	
	@Test
	public void evalMethodCall() {
		script.eval("function f(x, y) { return x + y }");
		Object sum = script.evalMethodCall("f", Arrays.asList(1, 2), x -> {}, x -> {});
		
		assertEquals(3.0, sum);
	}
	
	@Test
	public void isIterable_true() throws ScriptException {
		Object object = script.eval("[1, 2, 3]");
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
		Object object = script.eval("[1, 2, 3]");
		
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
		Object object = script.eval("({a: 'b'})");
		
		Map<Object, Object> map = script.getPropertyMap(object);
		
		assertEquals("b", map.get("a"));
	}
	
	@Test
	public void getPropertyMap_set() throws ScriptException {
		Object object = script.eval("({a: 'b'})");
		Map<Object, Object> map = script.getPropertyMap(object);
		
		map.put("a", "c");
		map.put("b", "d");
		
		assertEquals("c", script.getPropertyMap(object).get("a"));
		assertEquals("d", script.getPropertyMap(object).get("b"));
	}
	
	@Test
	public void isScriptObject_true() throws ScriptException {
		Object object = script.eval("({a: 'b'})");
		assertTrue(script.isScriptObject(object));
	}
	
	@Test
	public void isScriptObject_false() throws ScriptException {
		Object object = script.eval("10");
		assertFalse(script.isScriptObject(object));
	}
}