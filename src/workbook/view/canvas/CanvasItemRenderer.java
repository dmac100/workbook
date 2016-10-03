package workbook.view.canvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;

public class CanvasItemRenderer {
	private final ColorCache colorCache;

	public CanvasItemRenderer(ColorCache colorCache) {
		this.colorCache = colorCache;
	}
	
	/**
	 * Draws a single canvas item onto the graphics context.
	 */
	public void paint(GC gc, Transform transform, Bounds bounds, CanvasItem canvasItem) {
		String name = canvasItem.getName();
		
		double x = getDoubleOrDefault(canvasItem, "x", 0);
		double y = getDoubleOrDefault(canvasItem, "y", 0);
		double x1 = getDoubleOrDefault(canvasItem, "x1", 0);
		double y1 = getDoubleOrDefault(canvasItem, "y1", 0);
		double x2 = getDoubleOrDefault(canvasItem, "x2", 0);
		double y2 = getDoubleOrDefault(canvasItem, "y2", 0);
		double cx = getDoubleOrDefault(canvasItem, "cx", 0);
		double cy = getDoubleOrDefault(canvasItem, "cy", 0);
		double width = getDoubleOrDefault(canvasItem, "width", 50);
		double height = getDoubleOrDefault(canvasItem, "height", 50);
		double r = getDoubleOrDefault(canvasItem, "r", 0);
		double arcWidth = getDoubleOrDefault(canvasItem, "rx", 0) * 2;
		double arcHeight = getDoubleOrDefault(canvasItem, "ry", 0) * 2;
		double opacity = getDoubleOrDefault(canvasItem, "opacity", 1);
		int colorRed = (int) getDoubleOrDefault(canvasItem, "fill-red", 200);
		int colorGreen = (int) getDoubleOrDefault(canvasItem, "fill-green", 200);
		int colorBlue = (int) getDoubleOrDefault(canvasItem, "fill-blue", 200);
		int strokeRed = (int) getDoubleOrDefault(canvasItem, "stroke-red", 200);
		int strokeGreen = (int) getDoubleOrDefault(canvasItem, "stroke-green", 200);
		int strokeBlue = (int) getDoubleOrDefault(canvasItem, "stroke-blue", 200);
		int strokeWidth = (int) getDoubleOrDefault(canvasItem, "strokeWidth", 0);
		String strokeStyle = getStringOrDefault(canvasItem, "strokeStyle", "solid");
		double arrowLength = getDoubleOrDefault(canvasItem, "arrowLength", 0);
		double arrowAngle = getDoubleOrDefault(canvasItem, "arrowAngle", 40);
		double startOffset = getDoubleOrDefault(canvasItem, "startOffset", 0);
		double endOffset = getDoubleOrDefault(canvasItem, "endOffset", 0);
		String text = getStringOrDefault(canvasItem, "text", "");
		int fontSize = (int) getDoubleOrDefault(canvasItem, "fontSize", 12);
		String fontName = getStringOrDefault(canvasItem, "fontName", "Arial");
		String fontStyle = getStringOrDefault(canvasItem, "fontStyle", "normal");
		String textAlign = getStringOrDefault(canvasItem, "textAlign", "left");
		
		arrowAngle = ensureInRange(arrowAngle, 0, 90);
		
		gc.setAlpha((int)(opacity * 255));
		gc.setBackground(colorCache.getColor(colorRed, colorGreen, colorBlue));
		gc.setForeground(colorCache.getColor(strokeRed, strokeGreen, strokeBlue));
		gc.setLineWidth(strokeWidth);
		
		if(strokeStyle.equals("solid")) gc.setLineStyle(SWT.LINE_SOLID);
		if(strokeStyle.equals("dot")) gc.setLineStyle(SWT.LINE_DOT);
		if(strokeStyle.equals("dash")) gc.setLineStyle(SWT.LINE_DASH);
		if(strokeStyle.equals("dashdot")) gc.setLineStyle(SWT.LINE_DASHDOT);
		if(strokeStyle.equals("dashdotdot")) gc.setLineStyle(SWT.LINE_DASHDOTDOT);
		
		ScaledCanvas scaledCanvas = new ScaledCanvas(gc, transform, bounds);
		
		if(name.equals("rect")) {
			if(!canvasItem.hasProperty("rx")) arcWidth = arcHeight;
			if(!canvasItem.hasProperty("ry")) arcHeight = arcWidth;
			arcWidth = ensureInRange(arcWidth, 0, width);
			arcHeight = ensureInRange(arcHeight, 0, height);
			
			if(!getStringOrDefault(canvasItem, "fill", "").equals("none")) {
				scaledCanvas.fillRoundRectangle(x, y, width, height, arcWidth, arcHeight);
			}
			if(strokeWidth > 0) {
				scaledCanvas.drawRoundRectangle(x, y, width, height, arcWidth, arcHeight);
			}
		} else if(name.equals("ellipse")) {
			if(!getStringOrDefault(canvasItem, "fill", "").equals("none")) {
				scaledCanvas.fillOval(cx, cy, arcWidth * 2, arcHeight * 2);
			}
			if(strokeWidth > 0) {
				scaledCanvas.drawOval(cx, cy, arcWidth * 2, arcHeight * 2);
			}
		} else if(name.equals("circle")) {
			if(!getStringOrDefault(canvasItem, "fill", "").equals("none")) {
				scaledCanvas.fillOval(cx, cy, r * 2, r * 2);
			}
			if(strokeWidth > 0) {
				scaledCanvas.drawOval(cx, cy, r * 2, r * 2);
			}
		} else if(name.equals("line")) {
			if(startOffset > 0 || endOffset > 0) {
				double theta = Math.atan2(y2 - y1, x2 - x1);
				x1 += startOffset * Math.cos(theta);
				y1 += startOffset * Math.sin(theta);
				x2 -= endOffset * Math.cos(theta);
				y2 -= endOffset * Math.sin(theta);
			}
			
			if(arrowLength > 0) {
				gc.setBackground(colorCache.getColor(strokeRed, strokeGreen, strokeBlue));
				scaledCanvas.drawArrow(x1, y1, x2, y2, arrowLength, Math.toRadians(arrowAngle));
			} else {
				scaledCanvas.drawLine(x1, y1, x2, y2);
			}
		} else if(name.equals("text")) {
			int style = SWT.NORMAL;
			if(fontStyle.toLowerCase().contains("bold")) style |= SWT.BOLD;
			if(fontStyle.toLowerCase().contains("italic")) style |= SWT.ITALIC;
			Font font = new Font(gc.getDevice(), new FontData(fontName, fontSize, style));

			scaledCanvas.drawText(text, font, textAlign, x, y, true);
			
			font.dispose();
		}
		
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(0);
		gc.setAlpha(255);
	}

	private static double ensureInRange(double value, double min, double max) {
		if(value < min) return min;
		if(value > max) return max;
		return value;
	}

	private static double getDoubleOrDefault(CanvasItem canvasItem, String name, double defaultValue) {
		try {
			String value = canvasItem.getProperty(name);
			if(value != null) {
				return Double.parseDouble(value);
			}
		} catch(Exception e) {
		}
		
		return defaultValue;
	}

	private static String getStringOrDefault(CanvasItem canvasItem, String name, String defaultValue) {
		try {
			String value = canvasItem.getProperty(name);
			if(value != null) {
				return value;
			}
		} catch(Exception e) {
		}
		
		return defaultValue;
	}
}