package workbook.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;

import workbook.controller.MainController;
import workbook.editor.ui.Editor;
import workbook.view.TabbedViewLayout.FolderPosition;

/**
 * A view factory that allows registration of view factories by type, and then the
 * creation of views based of that type.
 */
public class TabbedViewFactory {
	public static class ViewInfo {
		private final Class<? extends TabbedView> type;
		private final String defaultTitle;
		private final FolderPosition defaultPosition;
		private final BiFunction<MainController, Composite, TabbedView> factory;
		
		public ViewInfo(Class<? extends TabbedView> type, String defaultTitle, FolderPosition defaultPosition, BiFunction<MainController, Composite, TabbedView> factory) {
			this.type = type;
			this.defaultTitle = defaultTitle;
			this.defaultPosition = defaultPosition;
			this.factory = factory;
		}

		public Class<? extends TabbedView> getType() {
			return type;
		}

		public String getDefaultTitle() {
			return defaultTitle;
		}

		public FolderPosition getDefaultPosition() {
			return defaultPosition;
		}

		public BiFunction<MainController, Composite, TabbedView> getFactory() {
			return factory;
		}
	}
	
	private final Map<String, ViewInfo> viewInfos = new LinkedHashMap<>();
	
	private final TabbedViewLayout tabbedViewLayout;
	private final MainController mainController;
	
	public TabbedViewFactory(TabbedViewLayout tabbedViewLayout, MainController mainController) {
		this.tabbedViewLayout = tabbedViewLayout;
		this.mainController = mainController;
	}
	
	/**
	 * Registers a new view type with a factory.
	 */
	public void registerView(Class<? extends TabbedView> type, String defaultTitle, FolderPosition defaultPosition, BiFunction<MainController, Composite, TabbedView> factory) {
		viewInfos.put(type.getSimpleName(), new ViewInfo(type, defaultTitle, defaultPosition, factory));
	}
	
	/**
	 * Returns a list of the registered types.
	 */
	public Collection<ViewInfo> getRegisteredViews() {
		return new ArrayList<>(viewInfos.values());
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