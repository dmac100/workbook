package layout;
import org.eclipse.swt.layout.FillLayout;

public class FillLayoutBuilder {
	private final FillLayout fillLayout = new FillLayout();
	
	public FillLayoutBuilder type(int type) {
		fillLayout.type = type;
		return this;
	}
	
	public FillLayoutBuilder marginWidth(int marginWidth) {
		fillLayout.marginWidth = marginWidth;
		return this;
	}
	
	public FillLayoutBuilder marginHeight(int marginHeight) {
		fillLayout.marginHeight = marginHeight;
		return this;
	}
	
	public FillLayoutBuilder spacing(int spacing) {
		fillLayout.spacing = spacing;
		return this;
	}
	
	public FillLayout build() {
		return fillLayout;
	}
}