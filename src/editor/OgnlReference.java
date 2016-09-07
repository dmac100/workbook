package editor;

import java.util.Map;

import ognl.DefaultClassResolver;
import ognl.DefaultMemberAccess;
import ognl.DefaultTypeConverter;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

public class OgnlReference implements Reference {
	private final Map<String, Object> rootObject;
	private final OgnlContext context;
	
	private Object expression = null;

	public OgnlReference(Map<String, Object> rootObject, String expression) {
		this.rootObject = rootObject;
		this.context = new OgnlContext(new DefaultClassResolver(), new DefaultTypeConverter(), new DefaultMemberAccess(true));
		try {
			this.expression = Ognl.parseExpression(expression);
		} catch (OgnlException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void set(Object value) {
		try {
			Ognl.setValue(expression, context, rootObject, value);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object get() {
		try {
			return Ognl.getValue(expression, context, rootObject);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}