package editor;

import java.lang.reflect.Method;

import script.Script;
import script.ScriptController;

public class JavaPropertyReference extends AbstractScriptReference {
	private final Method getMethod;
	private final Method setMethod;
	private final Object object;

	public JavaPropertyReference(ScriptController scriptController, Object object, Method getMethod, Method setMethod) {
		super(scriptController);
		
		this.object = object;
		this.getMethod = getMethod;
		this.setMethod = setMethod;
	}
	
	@Override
	protected void setSync(Script script, Object value) throws Exception {
		if(setMethod != null) {
			setMethod.invoke(object, value);
		}
	}

	@Override
	protected Object getSync(Script script) throws Exception {
		if(getMethod == null) {
			return null;
		} else {
			return getMethod.invoke(object);
		}
	}
}
