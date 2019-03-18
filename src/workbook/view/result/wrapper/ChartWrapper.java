package workbook.view.result.wrapper;

public class ChartWrapper implements Wrapper {
	private final Object value;

	public ChartWrapper(Object value) {
		this.value = value;
	}
	
	public Object getValue() {
		return value;
	}
}