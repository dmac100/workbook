package editor;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;

import script.ScriptController;

public class JavaPropertyReferenceTest {
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
		
		public void setA(Object a) {
			this.a = a;
		}
		
		public Object getB() {
			return b;
		}
		
		public void setB(Object b) {
			this.b = b;
		}
	}
	
	@Test
	public void test() throws Exception {
		ScriptController scriptController = new ScriptController();
		scriptController.startQueueThread();
		JavaObject object = new JavaObject(1, 2);
		
		Method getMethod = object.getClass().getMethod("getA");
		Method setMethod = object.getClass().getMethod("setA", Object.class);
		Reference reference = new JavaPropertyReference(scriptController, object, getMethod, setMethod);
		
		assertEquals(1, reference.get().get());
		
		reference.set(2).get();
		
		assertEquals(2, reference.get().get());
	}
}
