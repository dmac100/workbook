package workbook.view.result;

import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.google.common.base.Predicate;

import workbook.MainView;
import workbook.editor.ui.Editor;
import workbook.view.TabbedView;
import workbook.view.result.wrapper.Wrapper;

/**
 * Renders results from either EditorResults or conditions in corresponding editor.
 */
public class EditorRenderer implements ResultRenderer {
	private final ResultRenderer next;
	
	private MainView mainView;
	private Predicate<Object> condition;
	private String editorType;
	private int height;

	/**
	 * Renders any EditorResults in the corresponding editor registered in mainView.
	 */
	public EditorRenderer(ResultRenderer next, MainView mainView) {
		this.next = next;
		this.mainView = mainView;
	}

	/**
	 * Renders results matching the condition in the editor of editorType.
	 */
	public EditorRenderer(ResultRenderer next, MainView mainView, Predicate<Object> condition, Class<? extends Editor> editorType, int height) {
		this.next = next;
		this.mainView = mainView;
		this.condition = condition;
		this.editorType = editorType.getSimpleName();
		this.height = height;
	}
	
	public void addView(Composite parent, Object value, boolean changed, Runnable callback) {
		EditorResult editorResult = getEditorResult(value);
		
		if(editorResult != null) {
			// Remove any existing results.
			for(Control control:parent.getChildren()) {
				control.dispose();
			}
			
			Function<Composite, TabbedView> viewFactory = mainView.getViewFactory(editorResult.getEditorType());
			
			if(viewFactory != null) {
				// Create fixed size composite.
				Composite composite = new Composite(parent, SWT.NONE) {
					public Point computeSize(int wHint, int hHint, boolean changed) {
						return new Point(800, editorResult.getHeight());
					}
				};
				
				composite.setLayout(new FillLayout());
				
				// Create editor and set value from result.
				TabbedView view = viewFactory.apply(composite);
				if(view instanceof Editor) {
					((Editor) view).setValue(editorResult.getValue());
				}
			
				callback.run();
				return;
			}
		}
		
		next.addView(parent, value, changed, callback);
	}

	private EditorResult getEditorResult(Object value) {
		if(value instanceof EditorResult) {
			return (EditorResult) value;
		} else if(condition != null) {
			if(condition.apply(value)) {
				return new EditorResult(value, editorType, height);
			}
		}
		
		return null;
	}
}