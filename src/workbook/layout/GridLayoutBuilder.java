package workbook.layout;

import org.eclipse.swt.layout.GridLayout;

/**
 * Builds a GridLayout.
 */
public class GridLayoutBuilder {
	private final GridLayout gridLayout = new GridLayout();
	
	public GridLayoutBuilder numColumns(int numColumns) {
		gridLayout.numColumns = numColumns;
		return this;
	}
	
	public GridLayoutBuilder makeColumnsEqualWidth(boolean makeColumnsEqualWidth) {
		gridLayout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		return this;
	}
	
	public GridLayoutBuilder horizontalSpacing(int horizontalSpacing) {
		gridLayout.horizontalSpacing = horizontalSpacing;
		return this;
	}
	
	public GridLayoutBuilder verticalSpacing(int verticalSpacing) {
		gridLayout.verticalSpacing = verticalSpacing;
		return this;
	}
	
	public GridLayoutBuilder marginWidth(int marginWidth) {
		gridLayout.marginWidth = marginWidth;
		return this;
	}
	
	public GridLayoutBuilder marginHeight(int marginHeight) {
		gridLayout.marginHeight = marginHeight;
		return this;
	}
	
	public GridLayoutBuilder marginLeft(int marginLeft) {
		gridLayout.marginLeft = marginLeft;
		return this;
	}
	
	public GridLayoutBuilder marginRight(int marginRight) {
		gridLayout.marginRight = marginRight;
		return this;
	}
	
	public GridLayoutBuilder marginTop(int marginTop) {
		gridLayout.marginTop = marginTop;
		return this;
	}
	
	public GridLayoutBuilder marginBottom(int marginBottom) {
		gridLayout.marginBottom = marginBottom;
		return this;
	}
	
	public GridLayout build() {
		return gridLayout;
	}
}