package workbook.view.canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.jdom2.Element;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import workbook.event.MajorRefreshEvent;
import workbook.event.MinorRefreshEvent;
import workbook.event.ScriptTypeChangeEvent;
import workbook.model.Model;
import workbook.script.NameAndProperties;
import workbook.view.TabbedView;
import workbook.view.text.EditorText;

public class CanvasTabbedView implements TabbedView {
	private final TabFolder folder;
	private final EditorText editorText;
	private final Canvas canvas;
	private final ColorCache colorCache;
	private final Model model;
	
	private final List<CanvasItem> canvasItems = new ArrayList<>();
	private Consumer<String> executeCallback;
	
	private Bounds bounds;
	private String boundsFit = "extend";
	
	public CanvasTabbedView(Composite parent, EventBus eventBus, Model model) {
		folder = new TabFolder(parent, SWT.BOTTOM);
		this.model = model;
		
		TabItem designTab = new TabItem(folder, SWT.NONE);
		designTab.setText("Design");
		this.editorText = new EditorText(folder);
		designTab.setControl(editorText.getControl());
		
		TabItem viewTab = new TabItem(folder, SWT.NONE);
		viewTab.setText("View");
		this.canvas = new Canvas(folder, SWT.DOUBLE_BUFFERED);
		viewTab.setControl(canvas);
		
		colorCache = new ColorCache(canvas.getDisplay());
		
		editorText.getStyledText().addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if(event.keyCode == SWT.CR && event.stateMask == SWT.CONTROL) {
					refresh();
					event.doit = false;
				}
			}
		});
		
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				paint(event.display, event.gc);
			}
		});
		
		folder.setSelection(1);
		
		refreshBrush();
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
	@Subscribe
	public void onScriptTypeChange(ScriptTypeChangeEvent event) {
		refreshBrush();
	}
	
	@Subscribe
	public void onMinorRefresh(MinorRefreshEvent event) {
		refresh();
	}
	
	@Subscribe
	public void onMajorRefresh(MajorRefreshEvent event) {
		refresh();
	}
	
	private void refreshBrush() {
		Display.getDefault().asyncExec(() -> editorText.setBrush(model.getBrush()));
	}
	
	private void paint(Display display, GC gc) {
		int canvasWidth = canvas.getBounds().width;
		int canvasHeight = canvas.getBounds().height;
		int canvasMargin = 6;
		
		// Draw background of canvas.
		gc.setBackground(colorCache.getColor(255, 255, 255));
		gc.fillRoundRectangle(canvasMargin / 2, canvasMargin / 2, canvasWidth - canvasMargin, canvasHeight - canvasMargin, 3, 3);
		
		// Clip to within the margin of the canvas.
		gc.setClipping(canvasMargin, canvasMargin, canvasWidth - canvasMargin * 2, canvasHeight - canvasMargin * 2);
		
		Transform transform = new Transform(display);
		transform.translate(canvasMargin, canvasMargin);
		if(bounds != null) {
			float boundsWidth = bounds.getMaxX() - bounds.getMinX();
			float boundsHeight = bounds.getMaxY() - bounds.getMinY();
			float canvasBoundsWidth = (canvasWidth - canvasMargin * 2);
			float canvasBoundsHeight = (canvasHeight - canvasMargin * 2);
			if(boundsFit.equals("extend")) {
				float scaleX = canvasBoundsWidth / boundsWidth;
				float scaleY = canvasBoundsHeight / boundsHeight;
				float scale = Math.min(scaleX, scaleY);
				transform.translate(-bounds.getMinX() * scale, -bounds.getMinY() * scale);
				transform.scale(scale, scale);
			} else if(boundsFit.equals("full")) {
				float scaleX = canvasBoundsWidth / boundsWidth;
				float scaleY = canvasBoundsHeight / boundsHeight;
				float scale = Math.min(scaleX, scaleY) * 0.95f;
				transform.translate(-bounds.getMinX() * scale, -bounds.getMinY() * scale);
				transform.translate(
					(boundsWidth * scale) * (scaleX / scale) / 2 - (boundsWidth / 2 * scale),
					(boundsHeight * scale) * (scaleY / scale) / 2 - (boundsHeight / 2 * scale)
				);
				transform.scale(scale, scale);
			}
		}
		
		// Paint the visual objects.
		CanvasItemRenderer renderer = new CanvasItemRenderer(colorCache);
		Bounds newBounds;
		if(boundsFit.equals("full")) {
			newBounds = new Bounds(Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE);
		} else {
			newBounds = new Bounds(canvasMargin, canvasMargin, canvasWidth - canvasMargin * 2, canvasHeight - canvasMargin * 2);
		}
		for(CanvasItem item:canvasItems) {
			renderer.paint(gc, transform, newBounds, item);
		}
		if(!newBounds.equals(bounds)) {
			canvas.redraw();
		}
		this.bounds = newBounds;
		transform.dispose();
		
		// Draw border around canvas.
		gc.setClipping(0, 0, canvasWidth, canvasHeight);
		gc.setForeground(colorCache.getColor(150, 150, 150));
		gc.drawRoundRectangle(canvasMargin / 2, canvasMargin / 2, canvasWidth - canvasMargin, canvasHeight - canvasMargin, 3, 3);
	}
	
	public void setExecuteCallback(Consumer<String> executeCallback) {
		this.executeCallback = executeCallback;
	}
	
	public void refresh() {
		canvas.getDisplay().asyncExec(() -> {
			if(executeCallback != null) {
				executeCallback.accept(editorText.getText());
			}
		});
	}
	
	public void setCanvasItems(List<NameAndProperties> values) {
		canvas.getDisplay().asyncExec(() -> {
			canvasItems.clear();
			for(NameAndProperties value:values) {
				canvasItems.add(new CanvasItem(value.getName(), value.getProperties()));
			}
			canvas.redraw();
		});
	}

	public Control getControl() {
		return folder;
	}

	public void serialize(Element element) {
		Element content = new Element("Content");
		content.setText(editorText.getText());
		element.addContent(content);
	}

	public void deserialize(Element element) {
		String content = element.getChildText("Content");
		editorText.setText(content);
	}
}
