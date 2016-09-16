package editor.reference;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
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
	
	private static class Types {
		private boolean primitive1;
		private byte primitive2;
		private char primitive3;
		private short primitive4;
		private int primitive5;
		private long primitive6;
		private float primitive7;
		private double primitive8;
		private boolean wrapper1;
		private Byte wrapper2;
		private Character wrapper3;
		private Short wrapper4;
		private Integer wrapper5;
		private Long wrapper6;
		private Float wrapper7;
		private Double wrapper8;
		private String string;

		public boolean getPrimitive1() {
			return primitive1;
		}

		public void setPrimitive1(boolean primitive1) {
			this.primitive1 = primitive1;
		}

		public byte getPrimitive2() {
			return primitive2;
		}

		public void setPrimitive2(byte primitive2) {
			this.primitive2 = primitive2;
		}

		public char getPrimitive3() {
			return primitive3;
		}

		public void setPrimitive3(char primitive3) {
			this.primitive3 = primitive3;
		}

		public short getPrimitive4() {
			return primitive4;
		}

		public void setPrimitive4(short primitive4) {
			this.primitive4 = primitive4;
		}

		public int getPrimitive5() {
			return primitive5;
		}

		public void setPrimitive5(int primitive5) {
			this.primitive5 = primitive5;
		}

		public long getPrimitive6() {
			return primitive6;
		}

		public void setPrimitive6(long primitive6) {
			this.primitive6 = primitive6;
		}

		public float getPrimitive7() {
			return primitive7;
		}

		public void setPrimitive7(float primitive7) {
			this.primitive7 = primitive7;
		}

		public double getPrimitive8() {
			return primitive8;
		}

		public void setPrimitive8(double primitive8) {
			this.primitive8 = primitive8;
		}

		public boolean getWrapper1() {
			return wrapper1;
		}

		public void setWrapper1(boolean wrapper1) {
			this.wrapper1 = wrapper1;
		}

		public Byte getWrapper2() {
			return wrapper2;
		}

		public void setWrapper2(Byte wrapper2) {
			this.wrapper2 = wrapper2;
		}

		public Character getWrapper3() {
			return wrapper3;
		}

		public void setWrapper3(Character wrapper3) {
			this.wrapper3 = wrapper3;
		}

		public Short getWrapper4() {
			return wrapper4;
		}

		public void setWrapper4(Short wrapper4) {
			this.wrapper4 = wrapper4;
		}

		public Integer getWrapper5() {
			return wrapper5;
		}

		public void setWrapper5(Integer wrapper5) {
			this.wrapper5 = wrapper5;
		}

		public Long getWrapper6() {
			return wrapper6;
		}

		public void setWrapper6(Long wrapper6) {
			this.wrapper6 = wrapper6;
		}

		public Float getWrapper7() {
			return wrapper7;
		}

		public void setWrapper7(Float wrapper7) {
			this.wrapper7 = wrapper7;
		}

		public Double getWrapper8() {
			return wrapper8;
		}

		public void setWrapper8(Double wrapper8) {
			this.wrapper8 = wrapper8;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}
	}
	
	private ScriptController scriptController = new ScriptController();
	
	@Before
	public void before() {
		scriptController.startQueueThread();
	}
	
	@Test
	public void simple() throws Exception {
		JavaObject object = new JavaObject(1, 2);
		
		Method getMethod = object.getClass().getMethod("getA");
		Method setMethod = object.getClass().getMethod("setA", Object.class);
		Reference reference = new JavaPropertyReference(scriptController, object, getMethod, setMethod);
		
		assertEquals(1, reference.get().get());
		
		reference.set(2).get();
		
		assertEquals(2, reference.get().get());
	}
	
	@Test
	public void typeConversion() throws Exception {
		Types types = new Types();

		testRoundTrip(types, "primitive1", "true");
		testRoundTrip(types, "primitive2", "1");
		testRoundTrip(types, "primitive3", "1");
		testRoundTrip(types, "primitive4", "1");
		testRoundTrip(types, "primitive5", "1");
		testRoundTrip(types, "primitive6", "1");
		testRoundTrip(types, "primitive7", "1.0");
		testRoundTrip(types, "primitive8", "1.0");
		testRoundTrip(types, "wrapper1", "true");
		testRoundTrip(types, "wrapper2", "1");
		testRoundTrip(types, "wrapper3", "1");
		testRoundTrip(types, "wrapper4", "1");
		testRoundTrip(types, "wrapper5", "1");
		testRoundTrip(types, "wrapper6", "1");
		testRoundTrip(types, "wrapper7", "1.0");
		testRoundTrip(types, "wrapper8", "1.0");
		testRoundTrip(types, "string", "1");
	}

	@Test
	public void typeConversion_nullValues() throws Exception {
		Types types = new Types();

		testRoundTrip(types, "wrapper2", "null");
		testRoundTrip(types, "wrapper3", "null");
		testRoundTrip(types, "wrapper4", "null");
		testRoundTrip(types, "wrapper5", "null");
		testRoundTrip(types, "wrapper6", "null");
		testRoundTrip(types, "wrapper7", "null");
		testRoundTrip(types, "wrapper8", "null");
		testRoundTrip(types, "string", "null");
	}

	private void testRoundTrip(Object object, String property, String value) throws InterruptedException, ExecutionException {
		Reference reference = createReference(object, property);
		reference.set(value).get();
		assertEquals(value, String.valueOf(reference.get().get()));
	}

	private JavaPropertyReference createReference(Object object, String property) {
		Method getMethod = null;
		Method setMethod = null;
		property = property.substring(0, 1).toUpperCase() + property.substring(1);
		for(Method method:object.getClass().getMethods()) {
			if(method.getName().matches("(is|get)" + property)) {
				getMethod = method;
			}
			if(method.getName().matches("set" + property)) {
				setMethod = method;
			}
		}
		return new JavaPropertyReference(scriptController, object, getMethod, setMethod);
	}
}