package workbook.editor;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.script.ScriptException;

import org.junit.Before;
import org.junit.Test;

import workbook.editor.ScriptTableUtil;
import workbook.editor.reference.Reference;
import workbook.script.GroovyEngine;
import workbook.script.RubyEngine;
import workbook.script.ScriptController;
import workbook.script.ScriptFuture;

public class RubyScriptTableUtilTest {
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
	
	private ScriptController scriptController = new ScriptController();
	
	private ScriptTableUtil scriptTableUtil;
	
	@Before
	public void before() {
		scriptController.startQueueThread();
		scriptController.addEngine("Ruby", new RubyEngine());
		scriptController.setScriptType("Ruby");
		scriptTableUtil = new ScriptTableUtil(scriptController);
	}

	private Map<String, List<String>> getTable(Object object) throws ScriptException, InterruptedException, ExecutionException {
		ScriptFuture<Map<String, List<Reference>>> table = scriptController.exec(() -> {
			return scriptTableUtil.getTable(object);
		});
		
		return resolveReferences(table.get());
	}
	
	@Test
	public void getTable_singleRubyObject() throws Exception {
		Map<String, List<String>> table = getTable(eval("({a: 1})"));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_rubyListSingleRubyObject() throws Exception {
		Map<String, List<String>> table = getTable(eval("[{a: 1}]"));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_rubyListMultipleRubyObject() throws Exception {
		Map<String, List<String>> table = getTable(eval("[{a: 1}, {b: 2}, {a: 3, b: 4}]"));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1", "null", "3"),
			"b", Arrays.asList("null", "2", "4")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_singleRubyNull() throws Exception {
		Map<String, List<String>> table = getTable(eval("nil"));
		
		Map<String, List<String>> expected = Map();
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_rubyListRubyNull() throws Exception {
		Map<String, List<String>> table = getTable(eval("[nil]"));
		
		Map<String, List<String>> expected = Map();
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_rubyListEmpty() throws Exception {
		Map<String, List<String>> table = getTable(eval("[]"));
		
		Map<String, List<String>> expected = Map();
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_singleJavaNull() throws Exception {
		Map<String, List<String>> table = getTable(null);
		
		Map<String, List<String>> expected = Map();
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_singleJavaObject() throws Exception {
		Map<String, List<String>> table = getTable(new JavaObject(1, 2));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1"),
			"b", Arrays.asList("2")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_javaListSingleJavaObject() throws Exception {
		Map<String, List<String>> table = getTable(Arrays.asList(new JavaObject(1, 2)));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1"),
			"b", Arrays.asList("2")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_javaListSingleJavaMap() throws Exception {
		Map<String, List<String>> table = getTable(Map("a", 1, "b", 2));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1"),
			"b", Arrays.asList("2")
		);
		
		assertEquals(expected, table);
	}
	
	@Test
	public void getTable_javaListMultipleJavaObject() throws Exception {
		Map<String, List<String>> table = getTable(Arrays.asList(new JavaObject(1, 2), new JavaObject(3, 4)));
		
		Map<String, List<String>> expected = Map(
			"a", Arrays.asList("1", "3"),
			"b", Arrays.asList("2", "4")
		);
		
		assertEquals(expected, table);
	}
	
	private Object eval(String expression) throws InterruptedException, ExecutionException {
		return scriptController.eval(expression, x -> {}, x -> {}).get();
	}
	
	private static Map<String, List<String>> resolveReferences(Map<String, List<Reference>> map) {
		Map<String, List<String>> resolvedMap = new HashMap<>();
		map.forEach((k, v) -> {
			List<String> resolvedList = v.stream().map(RubyScriptTableUtilTest::resolveReference).collect(toList());
			resolvedMap.put(k, resolvedList);
		});
		return resolvedMap;
	}
	
	private static String resolveReference(Reference reference) {
		try {
			return String.valueOf((reference == null) ? null : reference.get().get());
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static <K, V> Map<K, V> Map(Object... values) {
		Map<K, V> map = new TreeMap<>();
		for(int i = 0; i < values.length; i += 2) {
			map.put((K) values[i], (V) values[i + 1]);
		}
		return map;
	}
}