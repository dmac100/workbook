package view;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;

import controller.MainController;
import editor.ui.Editor;
import editor.ui.StringEditor;
import editor.ui.TableEditor;
import editor.ui.TreeEditor;
import view.TabbedViewLayout.FolderPosition;
import view.canvas.CanvasView;

public class ViewFactory {
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
	
	public ViewFactory(TabbedViewLayout tabbedViewLayout, MainController mainController) {
		this.tabbedViewLayout = tabbedViewLayout;
		this.mainController = mainController;
		
		viewInfos.put("Worksheet", new ViewInfo("Worksheet", FolderPosition.LEFT, (controller, parent) -> controller.addWorksheet(new Worksheet(parent))));
		viewInfos.put("Script", new ViewInfo("Script", FolderPosition.LEFT, (controller, parent) -> controller.addScriptEditor(new ScriptEditor(parent))));
		viewInfos.put("Console", new ViewInfo("Console", FolderPosition.BOTTOM, (controller, parent) -> controller.addConsole(new Console(parent))));
		viewInfos.put("Canvas", new ViewInfo("Canvas", FolderPosition.RIGHT, (controller, parent) -> controller.addCanvasView(new CanvasView(parent))));
		
		viewInfos.put("StringEditor", new ViewInfo("StringEditor", FolderPosition.RIGHT, (controller, parent) -> controller.addEditor(new StringEditor(parent))));
		viewInfos.put("TableEditor", new ViewInfo("TableEditor", FolderPosition.RIGHT, (controller, parent) -> controller.addEditor(new TableEditor(parent, mainController.getScriptController()))));
		viewInfos.put("TreeEditor", new ViewInfo("TreeEditor", FolderPosition.RIGHT, (controller, parent) -> controller.addEditor(new TreeEditor(parent, mainController.getScriptController()))));
	}
	
	public TabbedView addView(String type) {
		return addView(type, null);
	}
	
	public TabbedView addView(String type, String expression) {
		ViewInfo viewInfo = viewInfos.get(type);
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
	
	public TabbedView addView(String type, CTabFolder folder, String title) {
		switch(type) {
			case "Worksheet": return addView(viewInfos.get("Worksheet"), folder, title);
			case "ScriptEditor": return addView(viewInfos.get("Script"), folder, title);
			case "Console": return addView(viewInfos.get("Console"), folder, title);
			case "CanvasView": return addView(viewInfos.get("Canvas"), folder, title);
			case "StringEditor": return addView(viewInfos.get("StringEditor"), folder, title);
			case "TableEditor": return addView(viewInfos.get("TableEditor"), folder, title);
			case "TreeEditor": return addView(viewInfos.get("StringEditor"), folder, title);
		}
		throw new IllegalArgumentException("Unknown type: " + type);
	}
	
	public TabbedView addView(ViewInfo viewInfo, CTabFolder folder, String title) {
		return tabbedViewLayout.addTab(folder, title, parent -> viewInfo.factory.apply(mainController, parent));
	}
}