package controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;

import editor.reference.OgnlReference;
import editor.ui.Editor;
import script.NameAndProperties;
import script.ScriptController;
import script.ScriptController.ScriptType;
import script.ScriptFuture;
import util.ThrottledConsumer;
import view.ConsoleTabbedView;
import view.ScriptTabbedView;
import view.WorksheetTabbedView;
import view.canvas.CanvasTabbedView;

public class MainController {
	private final ScriptController scriptController = new ScriptController();
	private final List<ConsoleTabbedView> consoles = new ArrayList<>();
	private final List<Editor> editors = new ArrayList<>();
	private final List<CanvasTabbedView> canvases = new ArrayList<>();
	
	private final Consumer<Object> evalConsumer;
	private final Consumer<Void> flushConsoleConsumer;
	
	private final StringBuilder outputBuffer = new StringBuilder();
	private final StringBuilder errorBuffer = new StringBuilder();
	
	public MainController() {
		scriptController.startQueueThread();
		
		evalConsumer = new ThrottledConsumer<>(100, true, result -> {
			Display.getDefault().asyncExec(
				() -> onEval(result)
			);
		});
		
		flushConsoleConsumer = new ThrottledConsumer<Void>(100, true, result -> flushConsole());
	}
	
	public void clear() {
		consoles.clear();
		editors.clear();
		canvases.clear();
	}

	public WorksheetTabbedView addWorksheet(WorksheetTabbedView worksheet) {
		worksheet.setExecuteFunction(command -> {
			ScriptFuture<Object> result = scriptController.eval(command, this::addOutput, this::addError);
			result.thenAccept(evalConsumer);
			return result;
		});
		return worksheet;
	}
	
	public ScriptTabbedView addScriptEditor(ScriptTabbedView scriptEditor) {
		scriptEditor.setExecuteCallback(command -> {
			ScriptFuture<Object> result = scriptController.eval(command, this::addOutput, this::addError);
			result.thenAccept(evalConsumer);
		});
		return scriptEditor;
	}
	
	private void onEval(Object result) {
		scriptController
			.setVariable("_", result)
			.thenRun(() -> {
				editors.forEach(Editor::readValue);
				canvases.forEach(CanvasTabbedView::refresh);
			});
	}
	
	private void addOutput(String output) {
		outputBuffer.append(output + "\n");
		flushConsoleConsumer.accept(null);
	}
	
	private void addError(String error) {
		errorBuffer.append(error + "\n");
		flushConsoleConsumer.accept(null);
	}
	
	private void flushConsole() {
		String output = outputBuffer.toString();
		String error = errorBuffer.toString();
		outputBuffer.setLength(0);
		errorBuffer.setLength(0);
		consoles.forEach(console -> console.addOutput(output));
		consoles.forEach(console -> console.addError(error));
	}
	
	public void clearConsole() {
		consoles.forEach(console -> console.clear());
	}

	public ConsoleTabbedView addConsole(ConsoleTabbedView console) {
		consoles.add(console);
		return console;
	}
	
	public CanvasTabbedView addCanvasView(CanvasTabbedView canvas) {
		canvases.add(canvas);
		canvas.setExecuteCallback(command -> {
			List<String> callbackNames = Arrays.asList("rect", "ellipse", "fill", "circle", "line", "text");
			ScriptFuture<List<NameAndProperties>> result = scriptController.evalWithCallbackFunctions(command, callbackNames, this::addOutput, this::addError);
			result.thenAccept(value -> {
				canvas.setCanvasItems(value);
			});
		});
		return canvas;
	}

	public <T extends Editor> T addEditor(T editor) {
		editor.setReferenceFunction(expression -> new OgnlReference(scriptController, expression));
		editors.add(editor);
		return editor;
	}
	
	public void interrupt() {
		scriptController.interrupt();
	}

	public ScriptController getScriptController() {
		return scriptController;
	}

	public void setEngine(ScriptType script) {
		scriptController.setScriptType(script);
	}

	public void serialize(Element element) {
		Element scriptTypeElement = new Element("ScriptType");
		scriptTypeElement.setText(scriptController.getScriptType().toString());
		element.addContent(scriptTypeElement);
	}

	public void deserialize(Element element) {
		String scriptType = element.getChild("ScriptType").getText();
		scriptController.setScriptType(ScriptType.valueOf(scriptType));
	}
}