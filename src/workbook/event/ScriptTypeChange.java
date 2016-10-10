package workbook.event;

import syntaxhighlighter.brush.Brush;

public class ScriptTypeChange {
	private final String scriptType;
	private final Brush brush;
	
	public ScriptTypeChange(String scriptType, Brush brush) {
		this.scriptType = scriptType;
		this.brush = brush;
	}
	
	public String getScriptType() {
		return scriptType;
	}
	
	public Brush getBrush() {
		return brush;
	}
}
