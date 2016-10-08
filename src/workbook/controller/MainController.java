package workbook.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;

import workbook.editor.reference.GlobalVariableReference;
import workbook.editor.ui.Editor;
import workbook.script.NameAndProperties;
import workbook.script.ScriptController;
import workbook.script.ScriptController.ScriptType;
import workbook.script.ScriptFuture;
import workbook.util.ThrottledConsumer;
import workbook.view.ConsoleTabbedView;
import workbook.view.ScriptTabbedView;
import workbook.view.WorksheetTabbedView;
import workbook.view.canvas.CanvasTabbedView;

public class MainController {
	private final ScriptController scriptController = new ScriptController();
	
	private final List<Runnable> evalCallbacks = new ArrayList<>();
	private final List<ConsoleTabbedView> consoles = new ArrayList<>();
	
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
		evalCallbacks.clear();
		consoles.clear();
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
			.thenRun(() -> evalCallbacks.forEach(Runnable::run));
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
		console.getControl().addDisposeListener(event -> consoles.remove(console));
		consoles.add(console);
		return console;
	}
	
	public CanvasTabbedView addCanvasView(CanvasTabbedView canvas) {
		canvas.getControl().addDisposeListener(event -> evalCallbacks.remove(canvas));
		evalCallbacks.add(canvas::refresh);
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
		editor.setReferenceFunction(expression -> new GlobalVariableReference(scriptController, expression));
		editor.getControl().addDisposeListener(event -> evalCallbacks.remove(editor));
		evalCallbacks.add(editor::readValue);
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
	
	public void addVariable(String name, Object value) {
		scriptController.setVariable(name, value);
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