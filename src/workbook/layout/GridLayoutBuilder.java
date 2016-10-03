package workbook.layout;
import org.eclipse.swt.layout.GridLayout;

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
	
	public GridLayout build() {
		return gridLayout;
	}
}