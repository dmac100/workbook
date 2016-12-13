package workbook.event;

/**
 * An event dispatched when there is system output from the script.
 */
public class OutputEvent {
	private final String output;
	private final String error;
	
	public OutputEvent(String output, String error) {
		this.output = output;
		this.error = error;
	}
	
	public String getOutput() {
		return output;
	}
	
	public String getError() {
		return error;
	}
}
