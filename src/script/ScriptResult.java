package script;

public class ScriptResult {
	private String value;
	private String output;
	private String error;
	
	public ScriptResult(String value, String output, String error) {
		this.value = value;
		this.output = output;
		this.error = error;
	}

	public String getOutput() {
		return output;
	}

	public String getError() {
		return error;
	}

	public String getValue() {
		return value;
	}
}