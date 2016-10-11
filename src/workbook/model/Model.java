package workbook.model;

import syntaxhighlighter.brush.Brush;

public class Model {
	private String scriptType;
	private Brush brush;

	public String getScriptType() {
		return scriptType;
	}

	public void setScriptType(String scriptType) {
		this.scriptType = scriptType;
	}

	public Brush getBrush() {
		return brush;
	}

	public void setBrush(Brush brush) {
		this.brush = brush;
	}
}
