package workbook.event;

/**
 * An event dispatches when a minor refresh occurs. This should cauase editors without
 * side-effects to refresh their values.
 */
public class MinorRefreshEvent {
	private final Object source;

	public MinorRefreshEvent(Object source) {
		this.source = source;
	}
	
	public Object getSource() {
		return source;
	}
}
