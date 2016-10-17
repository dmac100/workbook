package workbook.layout;

import org.eclipse.swt.layout.GridData;

/**
 * Builds a GridData.
 */
public class GridDataBuilder {
	private final GridData gridData = new GridData();
	
	public GridDataBuilder fillHorizontal() {
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		return this;
	}
	
	public GridDataBuilder fillVertical() {
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		return this;
	}
	
	public GridDataBuilder horizontalAlignment(int horizontalAlignment) {
		gridData.horizontalAlignment = horizontalAlignment;
		return this;
	}
	
	public GridDataBuilder verticalAlignment(int verticalAlignment) {
		gridData.verticalAlignment = verticalAlignment;
		return this;
	}
	
	public GridDataBuilder grabExcessHorizontalSpace(boolean grabExcessHorizontalSpace) {
		gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
		return this;
	}
	
	public GridDataBuilder grabExcessVerticalSpace(boolean grabExcessVerticalSpace) {
		gridData.grabExcessVerticalSpace = grabExcessVerticalSpace;
		return this;
	}
	
	public GridDataBuilder horizontalSpan(int horizontalSpan) {
		gridData.horizontalSpan = horizontalSpan;
		return this;
	}
	
	public GridDataBuilder verticalSpan(int verticalSpan) {
		gridData.verticalSpan = verticalSpan;
		return this;
	}
	
	public GridData build() {
		return gridData;
	}
}