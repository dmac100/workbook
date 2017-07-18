package workbook.editor.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;

import syntaxhighlighter.brush.Brush;
import syntaxhighlighter.brush.BrushAS3;
import syntaxhighlighter.brush.BrushAppleScript;
import syntaxhighlighter.brush.BrushBash;
import syntaxhighlighter.brush.BrushCSharp;
import syntaxhighlighter.brush.BrushColdFusion;
import syntaxhighlighter.brush.BrushCpp;
import syntaxhighlighter.brush.BrushCss;
import syntaxhighlighter.brush.BrushDelphi;
import syntaxhighlighter.brush.BrushDiff;
import syntaxhighlighter.brush.BrushErlang;
import syntaxhighlighter.brush.BrushGroovy;
import syntaxhighlighter.brush.BrushJScript;
import syntaxhighlighter.brush.BrushJava;
import syntaxhighlighter.brush.BrushJavaFX;
import syntaxhighlighter.brush.BrushPerl;
import syntaxhighlighter.brush.BrushPhp;
import syntaxhighlighter.brush.BrushPlain;
import syntaxhighlighter.brush.BrushPowerShell;
import syntaxhighlighter.brush.BrushPython;
import syntaxhighlighter.brush.BrushRuby;
import syntaxhighlighter.brush.BrushSass;
import syntaxhighlighter.brush.BrushScala;
import syntaxhighlighter.brush.BrushSql;
import syntaxhighlighter.brush.BrushVb;
import syntaxhighlighter.brush.BrushXml;
import workbook.event.MinorRefreshEvent;
import workbook.script.ScriptController;
import workbook.util.ThrottledConsumer;
import workbook.view.TabbedView;
import workbook.view.text.EditorText;

/**
 * An editor that shows and allows editing of a String.
 */
public class StringTabbedEditor extends Editor implements TabbedView {
	private final Composite parent;
	private final EventBus eventBus;
	private final EditorText editorText;
	
	private final Consumer<Void> refreshConsumer;
	
	private boolean disableModifyCallback;
	
	public StringTabbedEditor(Composite parent, EventBus eventBus, ScriptController scriptController) {
		super(eventBus, scriptController);
		
		this.parent = parent;
		this.eventBus = eventBus;
		
		this.editorText = new EditorText(parent);
		
		editorText.setBrushes(getBrushes());
		
		editorText.getStyledText().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				if(!disableModifyCallback) {
					writeReference();
				}
			}
		});
		
		refreshConsumer = new ThrottledConsumer<Void>(500, true, result -> {
			eventBus.post(new MinorRefreshEvent(this));
		});
		
		registerEvents();
	}
	
	private static Map<String, Brush> getBrushes() {
		Map<String, Brush> brushes = new LinkedHashMap<>();
		brushes.put("AppleScript", new BrushAppleScript());
		brushes.put("AS3", new BrushAS3());
		brushes.put("Bash", new BrushBash());
		brushes.put("ColdFusion", new BrushColdFusion());
		brushes.put("Cpp", new BrushCpp());
		brushes.put("CSharp", new BrushCSharp());
		brushes.put("Css", new BrushCss());
		brushes.put("Delphi", new BrushDelphi());
		brushes.put("Diff", new BrushDiff());
		brushes.put("Erlang", new BrushErlang());
		brushes.put("Groovy", new BrushGroovy());
		brushes.put("Java", new BrushJava());
		brushes.put("JavaFX", new BrushJavaFX());
		brushes.put("JScript", new BrushJScript());
		brushes.put("Perl", new BrushPerl());
		brushes.put("Php", new BrushPhp());
		brushes.put("Plain", new BrushPlain());
		brushes.put("PowerShell", new BrushPowerShell());
		brushes.put("Python", new BrushPython());
		brushes.put("Ruby", new BrushRuby());
		brushes.put("Sass", new BrushSass());
		brushes.put("Scala", new BrushScala());
		brushes.put("Sql", new BrushSql());
		brushes.put("Vb", new BrushVb());
		brushes.put("Xml", new BrushXml());
		return brushes;
	}

	public void setValue(Object value) {
		if(value instanceof String) {
			Display.getDefault().asyncExec(() -> {
				if(!editorText.getControl().isDisposed()) {
					disableModifyCallback = true;
					if(!editorText.getText().equals(value)) {
						editorText.setText((String)value);
					}
					disableModifyCallback = false;
				}
			});
		}
	}
	
	public void writeReference() {
		if(reference != null) {
			reference.set(editorText.getText()).thenAccept(refreshConsumer);
		}
	}
	
	public void serialize(Element element) {
		super.serialize(element);
		Element brush = new Element("Brush");
		brush.setText(getBrushName(editorText.getBrush()));
		element.addContent(brush);
	}

	private static String getBrushName(Brush brush) {
		Map<String, Brush> filtered = Maps.filterValues(getBrushes(), v -> v.getClass() == brush.getClass());
		return (filtered.isEmpty()) ? null : filtered.keySet().iterator().next();
	}

	public void deserialize(Element element) {
		super.deserialize(element);
		Brush brush = getBrushes().get(element.getChildText("Brush"));
		if(brush != null) {
			editorText.setBrush(brush);
		}
	}

	public Control getControl() {
		return editorText.getControl();
	}
}