package editor;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

import editor.ScriptTableUtil;
import script.Script;

public class ScriptTableUtilTest {
	private static class JavaObject {
		private Object a;
		private Object b;
		
		public JavaObject(Object a, Object b) {
			this.a = a;
			this.b = b;
		}
		
		public Object getA() {
			return a;
		}
		
		public Object getB() {
			return b;
		}
	}
	
	private static final String JAVAOBJECT_CLASS = new JavaObject(null, null).getClass().toString();
	
	private final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
	
	private ScriptTableUtil scriptTableUtil = new ScriptTableUtil(new Script());

	@Test
	public void getTable_singleJsObject() throws Exception {
		Map<String, List<String>> table = scriptTableUtil.getTable(scriptEngine.eval("({a: 1})"));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_jsListSingleJsObject() throws Exception {
		Map<String, List<String>> table = scriptTableUtil.getTable(scriptEngine.eval("[{a: 1}]"));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_jsListMultipleJsObject() throws Exception {
		Map<String, List<String>> table = scriptTableUtil.getTable(scriptEngine.eval("[{a: 1}, {b: 2}, {a: 3, b: 4}]"));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1", "null", "3"),
			"b", Arrays.asList("null", "2", "4")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_singleJsNull() throws Exception {
		Map<String, List<String>> table = scriptTableUtil.getTable(scriptEngine.eval("null"));
		
		Map<String, List<String>> expected = Map();
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_jsListJsNull() throws Exception {
		Map<String, List<String>> table = scriptTableUtil.getTable(scriptEngine.eval("[null]"));
		
		Map<String, List<String>> expected = Map();
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_jsListEmpty() throws Exception {
		Map<String, List<String>> table = scriptTableUtil.getTable(scriptEngine.eval("[]"));
		
		Map<String, List<String>> expected = Map();
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_singleJavaNull() throws ScriptException {
		Map<String, List<String>> table = scriptTableUtil.getTable(null);
		
		Map<String, List<String>> expected = Map();
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_singleJavaObject() throws ScriptException {
		Map<String, List<String>> table = scriptTableUtil.getTable(new JavaObject(1, 2));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1"),
			"b", Arrays.asList("2"),
			"class", Arrays.asList(JAVAOBJECT_CLASS)
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_javaListSingleJavaObject() throws ScriptException {
		Map<String, List<String>> table = scriptTableUtil.getTable(Arrays.asList(new JavaObject(1, 2)));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1"),
			"b", Arrays.asList("2"),
			"class", Arrays.asList(JAVAOBJECT_CLASS)
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_javaListSingleJavaMap() throws ScriptException {
		Map<String, List<String>> table = scriptTableUtil.getTable(Arrays.asList(Map("a", 1, "b", 2)));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1"),
			"b", Arrays.asList("2")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_javaListMultipleJavaObject() throws ScriptException {
		Map<String, List<String>> table = scriptTableUtil.getTable(Arrays.asList(new JavaObject(1, 2), new JavaObject(3, 4)));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1", "3"),
			"b", Arrays.asList("2", "4"),
			"class", Arrays.asList(JAVAOBJECT_CLASS, JAVAOBJECT_CLASS)
		);
		
		assertEquals(expected, table);
	}
	
	private static <K, V> Map<K, V> Map(Object... values) {
		Map<K, V> map = new TreeMap<>();
		for(int i = 0; i < values.length; i += 2) {
			map.put((K) values[i], (V) values[i + 1]);
		}
		return map;
	}
}