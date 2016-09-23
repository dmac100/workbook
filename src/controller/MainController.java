package controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;

import editor.reference.OgnlReference;
import editor.ui.Editor;
import script.NameAndProperties;
import script.ScriptController;
import script.ScriptFuture;
import util.ThrottledConsumer;
import view.Console;
import view.ScriptEditor;
import view.Worksheet;
import view.canvas.CanvasView;

public class MainController {
	private final ScriptController scriptController = new ScriptController();
	private final List<Console> consoles = new ArrayList<>();
	private final List<Editor> editors = new ArrayList<>();
	private final List<CanvasView> canvases = new ArrayList<>();
	
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

	public void addWorksheet(Worksheet worksheet) {
		worksheet.setExecuteFunction(command -> {
			ScriptFuture<Object> result = scriptController.eval(command, this::addOutput, this::addError);
			result.thenAccept(evalConsumer);
			return result;
		});
	}
	
	public void addScriptEditor(ScriptEditor scriptEditor) {
		scriptEditor.setExecuteCallback(command -> {
			ScriptFuture<Object> result = scriptController.eval(command, this::addOutput, this::addError);
			result.thenAccept(evalConsumer);
		});
	}
	
	private void onEval(Object result) {
		scriptController
			.setVariable("_", result)
			.thenRun(() -> {
				editors.forEach(Editor::readValue);
				canvases.forEach(CanvasView::refresh);
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

	public void addConsole(Console console) {
		consoles.add(console);
	}
	
	public void addCanvasView(CanvasView canvas) {
		canvases.add(canvas);
		canvas.setExecuteCallback(command -> {
			List<String> callbackNames = Arrays.asList("rect", "ellipse", "fill", "circle", "line", "text");
			ScriptFuture<List<NameAndProperties>> result = scriptController.evalWithCallbackFunctions(command, callbackNames, this::addOutput, this::addError);
			result.thenAccept(value -> {
				canvas.setCanvasItems(value);
			});
		});
	}

	public void addEditor(Editor editor) {
		editor.setReferenceFunction(expression -> new OgnlReference(scriptController, expression));
		editors.add(editor);
	}
	
	public void interrupt() {
		scriptController.interrupt();
	}

	public ScriptController getScriptController() {
		return scriptController;
	}
}