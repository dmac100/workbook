package workbook.view.result;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import workbook.script.ScriptController;
import workbook.view.FontList;

public class StringRenderer implements ResultRenderer {
	private final ResultRenderer next;
	private final ScriptController scriptController;

	public StringRenderer(ResultRenderer next, ScriptController scriptController) {
		this.next = next;
		this.scriptController = scriptController;
	}
	
	public void addView(Composite parent, Object value, Runnable callback) {
		scriptController.exec(() -> {
			String valueString = String.valueOf(value);
			
			Display.getDefault().asyncExec(() -> {
				StyledText styledText = new StyledText(parent, SWT.NONE);
				styledText.setFont(FontList.consolas10);
				styledText.setEditable(false);
				styledText.setText(String.valueOf(value));
				
				callback.run();
			});
			
			return null;
		});
	}
}
