package view;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;

import controller.MainController;
import editor.ui.Editor;
import view.TabbedViewLayout.FolderPosition;

/**
 * A view factory that allows registration of view factories by type, and then the
 * creation of views based of that type.
 */
public class TabbedViewFactory {
	public static class ViewInfo {
		private final String defaultTitle;
		private final FolderPosition defaultPosition;
		private final BiFunction<MainController, Composite, TabbedView> factory;
		
		public ViewInfo(String defaultTitle, FolderPosition defaultPosition, BiFunction<MainController, Composite, TabbedView> factory) {
			this.defaultTitle = defaultTitle;
			this.defaultPosition = defaultPosition;
			this.factory = factory;
		}
	}
	
	private final Map<String, ViewInfo> viewInfos = new HashMap<>();
	
	private final TabbedViewLayout tabbedViewLayout;
	private final MainController mainController;
	
	public TabbedViewFactory(TabbedViewLayout tabbedViewLayout, MainController mainController) {
		this.tabbedViewLayout = tabbedViewLayout;
		this.mainController = mainController;
	}
	
	/**
	 * Registers a new view type with a factory.
	 */
	public void register(Class<? extends TabbedView> type, String defaultTitle, FolderPosition defaultPosition, BiFunction<MainController, Composite, TabbedView> factory) {
		viewInfos.put(type.getSimpleName(), new ViewInfo(defaultTitle, defaultPosition, factory));
	}

	/**
	 * Creates a new view of the given type.
	 */
	public TabbedView addView(Class<? extends TabbedView> type) {
		return addView(type, null);
	}

	/**
	 * Creates a new view of the given type, with an expression.
	 */
	public TabbedView addView(Class<? extends TabbedView> type, String expression) {
		ViewInfo viewInfo = viewInfos.get(type.getSimpleName());
		CTabFolder folder = tabbedViewLayout.getFolder(viewInfo.defaultPosition);
		String title = viewInfo.defaultTitle + ((expression == null) ? "" : ": " + expression);
		return tabbedViewLayout.addTab(folder, title, parent -> {
			TabbedView view = viewInfo.factory.apply(mainController, parent);
			if(view instanceof Editor) {
				((Editor) view).setExpression(expression);
			}
			return view;
		});
	}
	
	/**
	 * Creates a new view of the given type, in a specific folder.
	 */
	public TabbedView addView(String type, CTabFolder folder, String title) {
		ViewInfo viewInfo = viewInfos.get(type);
		return tabbedViewLayout.addTab(folder, title, parent -> viewInfo.factory.apply(mainController, parent));
	}
}