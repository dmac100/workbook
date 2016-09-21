package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;

import editor.reference.OgnlReference;
import editor.ui.Editor;
import script.ScriptController;
import script.ScriptFuture;
import util.ThrottledConsumer;
import view.Console;
import view.ScriptEditor;
import view.Worksheet;

public class MainController {
	private final ScriptController scriptController = new ScriptController();
	private final List<Console> consoles = new ArrayList<>();
	private final List<Editor> editors = new ArrayList<>();
	
	private final Consumer<Object> evalConsumer;
	
	public MainController() {
		scriptController.startQueueThread();
		
		evalConsumer = new ThrottledConsumer<>(100, true, result -> {
			Display.getDefault().asyncExec(
				() -> onEval(result)
			);
		});
	}
	
	public void clear() {
		consoles.clear();
		editors.clear();
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
			.thenRun(() -> editors.forEach(Editor::readValue));
	}

	private void addOutput(String output) {
		consoles.forEach(console -> console.addOutput(output));
	}
	
	private void addError(String error) {
		consoles.forEach(console -> console.addError(error));
	}

	public void addConsole(Console console) {
		consoles.add(console);
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